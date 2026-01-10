package io.jadify.core.model;

public record Issue(
        Severity severity,
        String ruleId,
        String message,
        ElementRef element
) {}
