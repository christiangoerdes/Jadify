package io.jadify.core.scan;

import com.sun.source.tree.*;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import io.jadify.core.config.Config;
import io.jadify.core.model.ElementKind;
import io.jadify.core.model.ElementRef;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jadify.core.model.ElementKind.*;
import static javax.lang.model.element.Modifier.*;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

public class JavaSourceScanner implements Scanner {

    @Override
    public ScanContext scan(Path projectRoot, Config config) throws IOException {
        // Resolve the configured source root relative to the project root.
        Path sourceRoot = projectRoot.resolve(config.projectRoot());
        if (!Files.exists(sourceRoot)) {
            return new ScanContext(config, List.of(), Map.of());
        }

        List<Path> javaFiles = listJavaFiles(sourceRoot);
        if (javaFiles.isEmpty()) {
            return new ScanContext(config, List.of(), Map.of());
        }

        JavaCompiler compiler = getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("No system Java compiler available (are you running on a JRE instead of a JDK?)");
        }

        List<ElementRef> elements = new ArrayList<>();
        Map<ElementRef, String> docComments = new HashMap<>();
        ScanFilters filters = ScanFilters.from(config.scan());

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromPaths(javaFiles);
            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    null,
                    List.of("-proc:none"),
                    null,
                    fileObjects
            );
            Iterable<? extends CompilationUnitTree> compilationUnits = task.parse();
            DocTrees docTrees = DocTrees.instance(task);

            for (CompilationUnitTree unit : compilationUnits) {
                String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
                if (!filters.packages().matches(packageName)) {
                    continue;
                }
                String sourceFile = toSourceFile(projectRoot, unit);
                new ScannerVisitor(config, filters, docTrees, packageName, sourceFile, elements, docComments)
                        .scan(unit, null);
            }
        }

        return new ScanContext(config, elements, docComments);
    }

    private static List<Path> listJavaFiles(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static boolean matchesAnnotations(List<? extends AnnotationTree> annotations, PatternFilter filter) {
        if (!matchesAnyAnnotation(annotations, filter.include())) {
            return false;
        }
        return !matchesAnyAnnotation(annotations, filter.exclude());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean matchesAnyAnnotation(List<? extends AnnotationTree> annotations, List<Pattern> patterns) {
        if (patterns.isEmpty()) {
            return true;
        }
        for (AnnotationTree annotation : annotations) {
            String annotationName = annotation.getAnnotationType().toString();
            for (Pattern pattern : patterns) {
                if (pattern.matcher(annotationName).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String toSourceFile(Path projectRoot, CompilationUnitTree unit) {
        if (unit.getSourceFile() == null || unit.getSourceFile().toUri() == null) {
            return "";
        }
        Path path = Path.of(unit.getSourceFile().toUri());
        if (path.startsWith(projectRoot)) {
            return projectRoot.relativize(path).toString();
        }
        return path.toString();
    }

    private static boolean isPublicApi(Set<Modifier> modifiers) {
        return modifiers.contains(PUBLIC) || modifiers.contains(PROTECTED); // TODO keep PROTECTED?
    }

    private static ElementKind kindFor(ClassTree tree) {
        return switch (tree.getKind()) {
            case INTERFACE -> INTERFACE;
            case ENUM -> ENUM;
            case ANNOTATION_TYPE -> ANNOTATION;
            default -> CLASS;
        };
    }

    // TODO extract and refactor
    private static final class ScannerVisitor extends TreePathScanner<Void, Void> {
        private final Config config;
        private final ScanFilters filters;
        private final DocTrees docTrees;
        private final String packageName;
        private final String sourceFile;
        private final List<ElementRef> elements;
        private final Map<ElementRef, String> docComments;
        private final Deque<ClassContext> classStack = new ArrayDeque<>();

        private ScannerVisitor(
                Config config,
                ScanFilters filters,
                DocTrees docTrees,
                String packageName,
                String sourceFile,
                List<ElementRef> elements,
                Map<ElementRef, String> docComments
        ) {
            this.config = config;
            this.filters = filters;
            this.docTrees = docTrees;
            this.packageName = packageName;
            this.sourceFile = sourceFile;
            this.elements = elements;
            this.docComments = docComments;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            String simpleName = node.getSimpleName().toString();
            if (simpleName.isEmpty()) {
                return null;
            }

            boolean parentPublic = classStack.isEmpty() || classStack.peek().isPublicApi;
            boolean parentInterface = !classStack.isEmpty() && classStack.peek().isInterfaceLike;
            boolean isPublicApi = parentPublic && isPublicApi(node.getModifiers().getFlags());
            if (!isPublicApi && parentInterface && !node.getModifiers().getFlags().contains(PRIVATE)) {
                // Nested members in interfaces are implicitly public unless declared private.
                isPublicApi = true;
            }
            String qualifiedName = buildQualifiedName(simpleName);
            ElementKind elementKind = kindFor(node);

            boolean isInterfaceLike = node.getKind() == Tree.Kind.INTERFACE
                    || node.getKind() == Tree.Kind.ANNOTATION_TYPE;
            classStack.push(new ClassContext(simpleName, qualifiedName, isPublicApi, isInterfaceLike));

            if (matchesTypeKind(node, elementKind) && filters.typeNames().matches(qualifiedName) && matchesAnnotations(
                    node.getModifiers().getAnnotations(),
                    filters.typeAnnotations()
            ) && isPublicApi) {
                ElementRef ref = new ElementRef(
                        elementKind,
                        qualifiedName,
                        qualifiedName,
                        sourceFile
                );
                elements.add(ref);
                String doc = readDocComment();
                if (doc != null) {
                    docComments.put(ref, doc);
                }
            }

            super.visitClass(node, unused);
            classStack.pop();
            return null;
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            if (classStack.isEmpty()) {
                return null;
            }
            ClassContext context = classStack.peek();
            if (!context.isPublicApi) {
                return null;
            }

            boolean isConstructor = node.getReturnType() == null;
            boolean includeMember = isConstructor ? config.scan().members().include().constructors() : config.scan().members().include().methods();
            boolean methodPublic = isPublicApi(node.getModifiers().getFlags());
            if (!methodPublic && context.isInterfaceLike && !node.getModifiers().getFlags().contains(PRIVATE)) {
                // Interface methods are public by default.
                methodPublic = true;
            }
            if (!includeMember || !methodPublic) {
                return null;
            }

            String memberName = isConstructor ? context.simpleName : node.getName().toString();
            if (!filters.memberNames().matches(memberName)) {
                return null;
            }
            if (!matchesAnnotations(node.getModifiers().getAnnotations(), filters.memberAnnotations())) {
                return null;
            }

            String signature = buildSignature(memberName, node.getParameters());
            ElementKind kind = isConstructor ? ElementKind.CONSTRUCTOR : ElementKind.METHOD;
            String qualifiedName = context.qualifiedName + "#" + signature;
            String displayName = context.qualifiedName + "#" + signature;
            ElementRef ref = new ElementRef(kind, qualifiedName, displayName, sourceFile);
            elements.add(ref);
            String doc = readDocComment();
            if (doc != null) {
                docComments.put(ref, doc);
            }

            return null;
        }

        private String buildQualifiedName(String simpleName) {
            StringBuilder builder = new StringBuilder();
            if (!packageName.isEmpty()) {
                builder.append(packageName).append('.');
            }
            if (!classStack.isEmpty()) {
                for (var iterator = classStack.descendingIterator(); iterator.hasNext(); ) {
                    builder.append(iterator.next().simpleName).append('.');
                }
            }
            builder.append(simpleName);
            return builder.toString();
        }

        private boolean matchesTypeKind(ClassTree node, ElementKind kind) {
            Config.IncludeKinds include = config.scan().types().include();
            return switch (node.getKind()) {
                case INTERFACE -> include.interfaces();
                case ENUM -> include.enums();
                case ANNOTATION_TYPE -> include.annotations();
                case RECORD -> include.records();
                case CLASS -> include.classes() || (include.exceptions() && isException(node));
                default -> kind == CLASS && include.classes();
            };
        }

        private boolean isException(ClassTree node) {
            return node.getExtendsClause() != null
                    && node.getExtendsClause().toString().endsWith("Exception");
        }

        private String buildSignature(String name, List<? extends VariableTree> parameters) {
            StringBuilder builder = new StringBuilder();
            builder.append(name).append('(');
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(parameters.get(i).getType().toString());
            }
            builder.append(')');
            return builder.toString();
        }

        private String readDocComment() {
            TreePath path = getCurrentPath();
            if (path == null) {
                return null;
            }
            // DocTrees returns null when no doc comment is present.
            var doc = docTrees.getDocCommentTree(path);
            return doc == null ? null : doc.toString();
        }
    }

    private record ClassContext(String simpleName, String qualifiedName, boolean isPublicApi, boolean isInterfaceLike) {}

    private record PatternFilter(List<Pattern> include, List<Pattern> exclude) {
        boolean matches(String value) {
            if (!matchesAny(value, include)) {
                return false;
            }
            return !matchesAny(value, exclude);
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private static boolean matchesAny(String value, List<Pattern> patterns) {
            if (patterns.isEmpty()) {
                return true;
            }
            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).matches()) {
                    return true;
                }
            }
            return false;
        }

        static PatternFilter from(List<String> include, List<String> exclude) {
            return new PatternFilter(compilePatterns(include), compilePatterns(exclude));
        }

        private static List<Pattern> compilePatterns(List<String> patterns) {
            if (patterns == null || patterns.isEmpty()) {
                return List.of();
            }
            return patterns.stream().map(Pattern::compile).collect(Collectors.toList());
        }
    }

    private record ScanFilters(
            PatternFilter packages,
            PatternFilter typeNames,
            PatternFilter typeAnnotations,
            PatternFilter memberNames,
            PatternFilter memberAnnotations
    ) {
        static ScanFilters from(Config.Scan scan) {
            return new ScanFilters(
                    PatternFilter.from(scan.packages().include(), scan.packages().exclude()),
                    PatternFilter.from(scan.types().names().include(), scan.types().names().exclude()),
                    PatternFilter.from(scan.types().annotations().include(), scan.types().annotations().exclude()),
                    PatternFilter.from(scan.members().names().include(), scan.members().names().exclude()),
                    PatternFilter.from(scan.members().annotations().include(), scan.members().annotations().exclude())
            );
        }
    }
}
