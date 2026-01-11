package io.jadify.core.config;

import io.jadify.core.model.Severity;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

// TODO rename when?
public record Config(
        String projectRoot,
        Scan scan,
        Defaults defaults,
        List<Rule> rules,
        FailOn failOn
) {
    public record Scan(Packages packages, Types types, Members members) {}

    public record Packages(List<String> include, List<String> exclude) {}

    public record Types(
            IncludeKinds include,
            NameFilter names,
            AnnotationFilter annotations
    ) {}

    public record Members(
            IncludeMembers include,
            NameFilter names,
            AnnotationFilter annotations,
            boolean includeInherited
    ) {}

    public record IncludeKinds(
            boolean classes,
            boolean interfaces,
            boolean records,
            boolean enums,
            boolean annotations,
            boolean exceptions
    ) {}

    public record IncludeMembers(
            boolean fields,
            boolean methods,
            boolean constructors,
            boolean getters,
            boolean setters,
            boolean recordComponents
    ) {}

    /** Regex include/exclude. Empty include => "match all". */
    public record NameFilter(List<String> include, List<String> exclude) {}

    /** Regex include/exclude on annotation FQNs. Empty include => "match all". */
    public record AnnotationFilter(List<String> include, List<String> exclude) {}

    public record Defaults(
            SeverityProfile severity,
            List<AnnotationPolicy> annotationPolicies
    ) {}

    public record SeverityProfile(
            Map<Visibility, Severity> byVisibility,
            List<SeverityOverride> overrides
    ) {}

    public record SeverityOverride(Selector match, Severity severity) {}

    public record AnnotationPolicy(
            String annotationPattern,     // regex against annotation FQN
            List<Target> targets,
            Effect effect,
            List<String> rules,           // for SUPPRESS_RULES; null/empty => all
            Severity toSeverity,          // for SET_SEVERITY
            Integer shift                // for SHIFT_SEVERITY (e.g. -1) TODO keep this?
    ) {}

    public enum Effect { SUPPRESS, SUPPRESS_RULES, SET_SEVERITY, SHIFT_SEVERITY }

    public record Rule(
            String id,
            boolean enabled,
            Severity severity,            // null => use defaults/effective severity
            Selector when,                // null => global
            JsonNode config               // rule-specific config (free-form). Must Match rule implementation structure
    ) {}

    public record Selector(
            List<Target> targets,
            List<Visibility> visibility,
            List<MemberKind> memberKinds,
            List<AccessorKind> accessorKinds,
            List<String> packagePatterns,     // regex against package name (e.g. io.jadify.impl)
            List<String> fqnPatterns,         // regex against full symbol (e.g. io.jadify.Foo#bar(java.lang.String))
            List<String> simpleNamePatterns,  // regex against simple name (e.g. Foo, getValue)
            List<String> signaturePatterns,   // regex against signature string you define
            AnnotationFilter annotations
    ) {}

    public enum Target { TYPE, FIELD, METHOD, CONSTRUCTOR, RECORD_COMPONENT }
    public enum MemberKind { FIELD, METHOD, CONSTRUCTOR, RECORD_COMPONENT }
    public enum AccessorKind { GETTER, SETTER, BOOLEAN_GETTER }
    public enum Visibility { PUBLIC, PROTECTED, PACKAGE, PRIVATE }

    public record FailOn(Severity severity) {}
}
