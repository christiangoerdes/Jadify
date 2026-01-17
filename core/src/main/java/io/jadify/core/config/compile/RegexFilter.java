package io.jadify.core.config.compile;

/**
 * Include/exclude matching helper. Empty include means match all.
 */
public record RegexFilter(RegexSet includes, RegexSet excludes) {

    public boolean matches(String value) {
        if (excludes != null && excludes.matches(value)) {
            return false;
        }
        if (includes == null) {
            return true;
        }
        if (includes.patterns().isEmpty()) {
            return true;
        }
        return includes.matches(value);
    }
}
