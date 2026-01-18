package io.jadify.core.config.compile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.jadify.core.config.compile.RegexSet.fromIncludes;
import static org.junit.jupiter.api.Assertions.*;

class RegexSetTest {

    @Test
    void fromIncludesCompilesPatternsAndMatches() {
        RegexSet set = fromIncludes(List.of("foo.*", "bar"));

        assertEquals(2, set.patterns().size());
        assertTrue(set.matches("foo123"));
        assertTrue(set.matches("bar"));
        assertFalse(set.matches("baz"));
    }

    @Test
    void matchesReturnsFalseOnNullValue() {
        RegexSet set = fromIncludes(List.of("foo"));

        assertFalse(set.matches(null));
    }

    @Test
    void fromExcludesHandlesNullOrEmptyInput() {
        assertTrue(RegexSet.fromExcludes(null).patterns().isEmpty());
        assertTrue(RegexSet.fromExcludes(List.of()).patterns().isEmpty());
    }
}
