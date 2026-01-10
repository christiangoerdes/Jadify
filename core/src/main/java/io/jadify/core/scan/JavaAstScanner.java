package io.jadify.core.scan;

import com.sun.source.tree.*;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import io.jadify.core.config.Config;
import io.jadify.core.model.ElementKind;
import io.jadify.core.model.ElementRef;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.PROTECTED;

public class JavaAstScanner  implements Scanner {

    @Override
    public ScanContext scan(Path projectRoot, Config config) throws IOException {
        List<File> javaFiles = findJavaFiles(projectRoot);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler found. Run Jadify with a JDK, not a JRE.");
        }

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var units = fm.getJavaFileObjectsFromFiles(javaFiles);
            var options = List.of("-proc:none");

            JavacTask task = (JavacTask) compiler.getTask(null, fm, null, options, null, units);
            DocTrees docTrees = DocTrees.instance(task);

            Iterable<? extends CompilationUnitTree> asts = task.parse();

            List<ElementRef> elements = new ArrayList<>();
            Map<ElementRef, String> docs = new HashMap<>();

            for (CompilationUnitTree cut : asts) {
                String fileName = cut.getSourceFile().getName();

                cut.accept(new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitClass(ClassTree ct, Void unused) {
                        if (isInScope(ct.getModifiers(), config.scope()) && !isSuppressed(ct.getModifiers(), config)) {
                            ElementRef ref = new ElementRef(
                                    ElementKind.CLASS,
                                    guessQualifiedName(cut, ct.getSimpleName().toString()),
                                    ct.getSimpleName().toString(),
                                    fileName
                            );
                            elements.add(ref);
                            putDocIfPresent(docTrees, cut, ct, ref, docs);
                        }
                        return super.visitClass(ct, unused);
                    }

                    @Override
                    public Void visitMethod(MethodTree mt, Void unused) {
                        if (isInScope(mt.getModifiers(), config.scope()) && !isSuppressed(mt.getModifiers(), config)) {
                            // Minimal signature: name + param count (extend later with types)
                            String display = mt.getName() + "(" + mt.getParameters().size() + ")";
                            ElementRef ref = new ElementRef(
                                    ElementKind.METHOD,
                                    guessQualifiedName(cut, display),
                                    display,
                                    fileName
                            );
                            elements.add(ref);
                            putDocIfPresent(docTrees, cut, mt, ref, docs);
                        }
                        return super.visitMethod(mt, unused);
                    }
                }, null);
            }
            return new ScanContext(config, elements, docs);
        }
    }

    private static boolean isInScope(ModifiersTree modifiers, Config.Scope scope) {
        var flags = modifiers.getFlags();
        return (scope.includePublic() && flags.contains(PUBLIC))
                || (scope.includeProtected() && flags.contains(PROTECTED));
    }

    private static boolean isSuppressed(ModifiersTree modifiers, Config config) {
        var anns = modifiers.getAnnotations().stream()
                .map(a -> a.getAnnotationType().toString())
                .toList();

        for (String suppress : config.suppressAnnotations()) {
            if (anns.stream().anyMatch(a -> a.endsWith("." + suppress) || a.endsWith(suppress))) return true;
        }
        return false;
    }

    private static void putDocIfPresent(DocTrees docTrees, CompilationUnitTree cut, Tree tree,
                                        ElementRef ref, Map<ElementRef, String> docs) {
        TreePath path = docTrees.getPath(cut, tree);
        String doc = docTrees.getDocComment(path);
        if (doc != null) docs.put(ref, doc);
    }

    private static String guessQualifiedName(CompilationUnitTree cut, String simple) {
        String pkg = cut.getPackageName() == null ? "" : cut.getPackageName().toString();
        return pkg.isEmpty() ? simple : pkg + "." + simple;
    }

    private static List<File> findJavaFiles(Path projectRoot) throws IOException {
        if (!Files.exists(projectRoot)) return List.of();
        try (var s = Files.find(projectRoot, Integer.MAX_VALUE,
                (p, a) -> a.isRegularFile() && p.toString().endsWith(".java"))) {
            return s.map(Path::toFile).collect(Collectors.toList());
        }
    }
}
