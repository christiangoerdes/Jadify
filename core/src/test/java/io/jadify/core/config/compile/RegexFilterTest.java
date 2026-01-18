package io.jadify.core.config.compile;

import org.junit.jupiter.api.Test;

import static io.jadify.core.config.compile.RegexSet.fromExcludes;
import static io.jadify.core.config.compile.RegexSet.fromIncludes;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexFilterTest {

    @Test
    void matchesRejectsExcludedValues() {
        RegexFilter filter = new RegexFilter(
                fromIncludes(of("foo.*")),
                fromExcludes(of("foo.bar"))
        );

        assertTrue(filter.matches("foo.baz"));
        assertFalse(filter.matches("foo.bar"));
    }

    @Test
    void matchesAllowsAllWhenIncludesNullOrEmpty() {
        assertTrue(new RegexFilter(null, null).matches("anything"));
        assertTrue(new RegexFilter(fromIncludes(of()), null).matches("anything"));
    }
}
