package io.jadify.core.config.compile;

import io.jadify.core.config.Config;
import io.jadify.core.config.Config.*;
import io.jadify.core.model.Severity;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static io.jadify.core.config.Config.AccessorKind.GETTER;
import static io.jadify.core.config.Config.Effect.SUPPRESS;
import static io.jadify.core.config.Config.MemberKind.METHOD;
import static io.jadify.core.config.Config.Target.TYPE;
import static io.jadify.core.config.Config.Visibility.PUBLIC;
import static io.jadify.core.model.Severity.ERROR;
import static io.jadify.core.model.Severity.WARN;
import static org.junit.jupiter.api.Assertions.*;
import static tools.jackson.databind.node.JsonNodeFactory.instance;

class ConfigCompilerTest {

    @Test
    void compileBuildsCompiledConfigWithSelectorsAndFilters() {
        Selector overrideSelector = new Selector(
                List.of(TYPE),
                List.of(PUBLIC),
                List.of(METHOD),
                List.of(GETTER),
                List.of("com\\.example"),
                List.of("com\\.example\\.Foo"),
                List.of("Foo"),
                List.of("Foo#bar\\(\\)"),
                new AnnotationFilter(List.of("com.example.OverrideAnno"), List.of("com.example.Exclude"))
        );

        ObjectNode ruleConfig = instance.objectNode().put("limit", 3);

        CompiledConfig compiled = getCompiledConfig(overrideSelector, ruleConfig);

        assertEquals("root", compiled.projectRoot());
        assertTrue(compiled.scan().packages().includes().matches("com.example.test"));
        assertTrue(compiled.scan().memberAnnotations().includes().matches("com.example.MemberAnno"));
        assertTrue(compiled.scan().includeInherited());
        assertEquals(1, compiled.defaults().severity().overrides().size());
        assertEquals(1, compiled.defaults().annotationPolicies().size());
        assertEquals(2, compiled.rules().size());
        assertSame(ruleConfig, compiled.rules().getFirst().config());
        assertNull(compiled.rules().getFirst().when());
        assertNotNull(compiled.rules().get(1).when());
        assertEquals(1, compiled.rules().get(1).when().annotations().includes().patterns().size());
    }

    private static @NonNull CompiledConfig getCompiledConfig(Selector overrideSelector, ObjectNode ruleConfig) {
        return new ConfigCompiler().compile(new Config(
                "root",
                new Config.Scan(
                        new Packages(List.of("com.example.*"), List.of("com.example.ignore.*")),
                        new Config.Types(
                                new IncludeKinds(true, false, false, false, false, false),
                                new NameFilter(List.of("Type.*"), List.of("Ignore.*")),
                                new AnnotationFilter(List.of("com.example.TypeAnno"), List.of("com.example.IgnoreAnno"))
                        ),
                        new Config.Members(
                                new IncludeMembers(true, true, false, false, false, false),
                                new NameFilter(List.of("member.*"), List.of("ignore.*")),
                                new AnnotationFilter(List.of("com.example.MemberAnno"), List.of("com.example.IgnoreMemberAnno")),
                                true
                        )
                ),
                new Defaults(
                        new SeverityProfile(
                                Map.of(PUBLIC, WARN),
                                List.of(new SeverityOverride(overrideSelector, ERROR))
                        ),
                        List.of(new AnnotationPolicy(
                                "com.example.Policy",
                                List.of(TYPE),
                                SUPPRESS,
                                List.of("rule1"),
                                Severity.INFO,
                                1
                        ))
                ),
                List.of(
                        new Config.Rule("rule-a", true, WARN, null, ruleConfig),
                        new Config.Rule("rule-b", false, ERROR, overrideSelector, null)
                ),
                new FailOn(ERROR)
        ));
    }
}
