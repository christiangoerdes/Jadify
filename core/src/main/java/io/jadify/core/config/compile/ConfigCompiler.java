package io.jadify.core.config.compile;

import io.jadify.core.config.Config;

import java.util.List;
import java.util.Objects;

/**
 * Compiles raw config into a runtime-friendly representation with precompiled selectors/filters.
 */
public final class ConfigCompiler {

    public CompiledConfig compile(Config config) {
        Objects.requireNonNull(config, "config");

        var packages = compileNameFilter(config.scan().packages().include(), config.scan().packages().exclude());
        var typeNames = compileNameFilter(config.scan().types().names().include(), config.scan().types().names().exclude());
        var typeAnnotations = compileAnnotationFilter(
                config.scan().types().annotations().include(),
                config.scan().types().annotations().exclude()
        );
        var memberNames = compileNameFilter(config.scan().members().names().include(), config.scan().members().names().exclude());
        var memberAnnotations = compileAnnotationFilter(
                config.scan().members().annotations().include(),
                config.scan().members().annotations().exclude()
        );

        var scan = new CompiledConfig.CompiledScan(
                packages,
                typeNames,
                typeAnnotations,
                memberNames,
                memberAnnotations,
                config.scan().members().includeInherited()
        );

        var defaults = new CompiledConfig.CompiledDefaults(
                new CompiledConfig.CompiledSeverityProfile(
                        config.defaults().severity().byVisibility(),
                        config.defaults().severity().overrides().stream()
                                .map(o -> new CompiledConfig.CompiledSeverityOverride(
                                        compileSelector(o.match()),
                                        o.severity()
                                ))
                                .toList()
                ),
                config.defaults().annotationPolicies().stream()
                        .map(p -> new CompiledConfig.CompiledAnnotationPolicy(
                                RegexSet.fromIncludes(List.of(p.annotationPattern())),
                                p.targets(),
                                p.effect(),
                                p.rules(),
                                p.toSeverity(),
                                p.shift()
                        ))
                        .toList()
        );

        var rules = config.rules().stream()
                .map(r -> new CompiledConfig.CompiledRule(
                        r.id(),
                        r.enabled(),
                        r.severity(),
                        compileSelector(r.when()),
                        r.config()
                ))
                .toList();

        return new CompiledConfig(config.projectRoot(), scan, defaults, rules, config.failOn());
    }

    private static RegexFilter compileNameFilter(List<String> include, List<String> exclude) {
        return new RegexFilter(RegexSet.fromIncludes(include), RegexSet.fromExcludes(exclude));
    }

    private static RegexFilter compileAnnotationFilter(List<String> include, List<String> exclude) {
        return new RegexFilter(RegexSet.fromIncludes(include), RegexSet.fromExcludes(exclude));
    }

    private static CompiledConfig.CompiledSelector compileSelector(Config.Selector selector) {
        if (selector == null) {
            return null;
        }
        return new CompiledConfig.CompiledSelector(
                selector.targets(),
                selector.visibility(),
                selector.memberKinds(),
                selector.accessorKinds(),
                RegexSet.fromIncludes(selector.packagePatterns()),
                RegexSet.fromIncludes(selector.fqnPatterns()),
                RegexSet.fromIncludes(selector.simpleNamePatterns()),
                RegexSet.fromIncludes(selector.signaturePatterns()),
                selector.annotations() == null
                        ? null
                        : compileAnnotationFilter(selector.annotations().include(), selector.annotations().exclude())
        );
    }
}
