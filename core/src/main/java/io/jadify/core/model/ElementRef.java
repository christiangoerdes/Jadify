package io.jadify.core.model;

public record ElementRef(
        ElementKind kind,
        String qualifiedName,
        String displayName,
        String sourceFile
) {}
