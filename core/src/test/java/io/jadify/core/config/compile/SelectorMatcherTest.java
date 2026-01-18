package io.jadify.core.config.compile;

import io.jadify.core.config.compile.CompiledConfig.CompiledSelector;
import io.jadify.core.config.compile.SelectorMatcher.SelectorInput;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.jadify.core.config.Config.AccessorKind.GETTER;
import static io.jadify.core.config.Config.MemberKind.METHOD;
import static io.jadify.core.config.Config.Target.TYPE;
import static io.jadify.core.config.Config.Visibility.PRIVATE;
import static io.jadify.core.config.Config.Visibility.PUBLIC;
import static io.jadify.core.config.compile.RegexSet.fromIncludes;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorMatcherTest {

    private final SelectorMatcher matcher = new SelectorMatcher();

    @Test
    void matchesReturnsTrueForNullSelector() {
        SelectorInput input = new SelectorInput(
                Set.of(TYPE),
                PUBLIC,
                Set.of(METHOD),
                Set.of(GETTER),
                "com.example",
                "com.example.Foo",
                "Foo",
                "Foo#bar()",
                "com.example.baz"
        );

        assertTrue(matcher.matches(null, input));
    }

    @Test
    void matchesEvaluatesEnumsAndPatterns() {
        assertTrue(matcher.matches(
                new CompiledSelector(
                        of(TYPE),
                        of(PUBLIC),
                        of(METHOD),
                        of(GETTER),
                        fromIncludes(of("com\\.example")),
                        fromIncludes(of("com\\.example\\.Foo")),
                        fromIncludes(of("Foo")),
                        fromIncludes(of("Foo#bar\\(\\)")),
                        new RegexFilter(fromIncludes(of("com\\.example\\.baz")), null)
                ), new SelectorInput(
                        Set.of(TYPE),
                        PUBLIC,
                        Set.of(METHOD),
                        Set.of(GETTER),
                        "com.example",
                        "com.example.Foo",
                        "Foo",
                        "Foo#bar()",
                        "com.example.baz"
                ))
        );
    }

    @Test
    void matchesReturnsFalseOnMismatchedEnumOrPattern() {
        CompiledSelector selector = new CompiledSelector(
                of(TYPE),
                of(PUBLIC),
                of(METHOD),
                of(GETTER),
                fromIncludes(of("com\\.example")),
                fromIncludes(of("com\\.example\\.Foo")),
                fromIncludes(of("Foo")),
                fromIncludes(of("Foo#bar\\(\\)")),
                new RegexFilter(fromIncludes(of("com\\.example\\.baz")), null)
        );

        assertFalse(matcher.matches(
                selector,
                new SelectorInput(
                        Set.of(TYPE),
                        PRIVATE,
                        Set.of(METHOD),
                        Set.of(GETTER),
                        "com.example",
                        "com.example.Foo",
                        "Foo",
                        "Foo#bar()",
                        "com.example.baz"
                ))
        );
        assertFalse(matcher.matches(
                selector,
                new SelectorInput(
                        Set.of(TYPE),
                        PUBLIC,
                        Set.of(METHOD),
                        Set.of(GETTER),
                        "com.other",
                        "com.example.Foo",
                        "Foo",
                        "Foo#bar()",
                        "com.example.Ann"
                ))
        );
    }
}
