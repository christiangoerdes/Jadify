package io.jadify.core.config.compile;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Collection of compiled regex patterns with simple match helpers.
 */
public record RegexSet(List<Pattern> patterns) {

    public boolean matches(String value) {
        if (value == null) {
            return false;
        }
        return patterns.stream().anyMatch(p -> p.matcher(value).matches());
    }

    public static RegexSet fromIncludes(List<String> includes) {
        return fromExcludes(includes);
    }

    public static RegexSet fromExcludes(List<String> excludes) {
        if (excludes == null || excludes.isEmpty()) {
            return new RegexSet(List.of());
        }
        return new RegexSet(compile(excludes));
    }

    private static List<Pattern> compile(List<String> patterns) {
        return patterns.stream()
                .map(Pattern::compile)
                .toList();
    }
}
