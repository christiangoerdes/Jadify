package io.jadify.core.scan;

import io.jadify.core.config.JadifyConfig;
import io.jadify.core.model.ElementRef;

import java.util.List;
import java.util.Map;

public record ScanContext(
        JadifyConfig config,
        List<ElementRef> publicApiElements,
        Map<ElementRef, String> docComments
) {
}
