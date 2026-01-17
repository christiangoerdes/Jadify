package io.jadify.core.config.compile;

import io.jadify.core.config.Config;
import io.jadify.core.model.Severity;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Immutable, preprocessed config used by runtime layers (scanner/rules/engine).
 */
public record CompiledConfig(
        String projectRoot,
        CompiledScan scan,
        CompiledDefaults defaults,
        List<CompiledRule> rules,
        Config.FailOn failOn
) {
    public record CompiledScan(
            RegexFilter packages,
            RegexFilter typeNames,
            RegexFilter typeAnnotations,
            RegexFilter memberNames,
            RegexFilter memberAnnotations,
            boolean includeInherited
    ) {}

    public record CompiledDefaults(
            CompiledSeverityProfile severity,
            List<CompiledAnnotationPolicy> annotationPolicies
    ) {}

    public record CompiledSeverityProfile(
            Map<Config.Visibility, Severity> byVisibility,
            List<CompiledSeverityOverride> overrides
    ) {}

    public record CompiledSeverityOverride(
            CompiledSelector match,
            Severity severity
    ) {}

    public record CompiledAnnotationPolicy(
            RegexSet annotationPattern,
            List<Config.Target> targets,
            Config.Effect effect,
            List<String> rules,
            Severity toSeverity,
            Integer shift
    ) {}

    public record CompiledRule(
            String id,
            boolean enabled,
            Severity severity,
            CompiledSelector when,
            JsonNode config
    ) {}

    public record CompiledSelector(
            List<Config.Target> targets,
            List<Config.Visibility> visibility,
            List<Config.MemberKind> memberKinds,
            List<Config.AccessorKind> accessorKinds,
            RegexSet packagePatterns,
            RegexSet fqnPatterns,
            RegexSet simpleNamePatterns,
            RegexSet signaturePatterns,
            RegexFilter annotations
    ) {}
}
