package io.jadify.core.config.compile;

import io.jadify.core.config.Config;

import java.util.List;
import java.util.Set;

/**
 * Evaluates whether a selector applies to a given element descriptor.
 */
public final class SelectorMatcher {

    public boolean matches(CompiledConfig.CompiledSelector selector, SelectorInput input) {
        if (selector == null) {
            return true;
        }
        if (!matchesEnum(selector.targets(), input.targets())) {
            return false;
        }
        if (!matchesEnum(selector.visibility(), input.visibility())) {
            return false;
        }
        if (!matchesEnum(selector.memberKinds(), input.memberKinds())) {
            return false;
        }
        if (!matchesEnum(selector.accessorKinds(), input.accessorKinds())) {
            return false;
        }
        if (!matchesPatterns(selector.packagePatterns(), input.packageName())) {
            return false;
        }
        if (!matchesPatterns(selector.fqnPatterns(), input.fqn())) {
            return false;
        }
        if (!matchesPatterns(selector.simpleNamePatterns(), input.simpleName())) {
            return false;
        }
        if (!matchesPatterns(selector.signaturePatterns(), input.signature())) {
            return false;
        }
        return selector.annotations() == null
                || selector.annotations().matches(input.annotationFqn());
    }

    private static boolean matchesEnum(List<?> selectorValues, Set<?> inputValues) {
        if (selectorValues == null || selectorValues.isEmpty()) {
            return true;
        }
        if (inputValues == null || inputValues.isEmpty()) {
            return false;
        }
        for (Object value : selectorValues) {
            if (inputValues.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEnum(List<?> selectorValues, Object inputValue) {
        if (selectorValues == null || selectorValues.isEmpty()) {
            return true;
        }
        if (inputValue == null) {
            return false;
        }
        return selectorValues.contains(inputValue);
    }

    private static boolean matchesPatterns(RegexSet patterns, String value) {
        if (patterns == null || patterns.patterns().isEmpty()) {
            return true;
        }
        return patterns.matches(value);
    }

    public record SelectorInput(
            Set<Config.Target> targets,
            Config.Visibility visibility,
            Set<Config.MemberKind> memberKinds,
            Set<Config.AccessorKind> accessorKinds,
            String packageName,
            String fqn,
            String simpleName,
            String signature,
            String annotationFqn
    ) {}
}
