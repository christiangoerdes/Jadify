package io.jadify.core.config;

import io.jadify.core.model.Severity;

import java.util.List;

public record JadifyConfig(
        String projectRoot,
        List<String> includePackages,
        List<String> excludePackages,
        List<String> suppressAnnotations,
        Scope scope,
        List<RuleToggle> rules,
        FailOn failOn
) {
    public record Scope(boolean includePublic, boolean includeProtected) {}
    public record RuleToggle(String id, boolean enabled) {}
    public record FailOn(Severity severity) {}
}
