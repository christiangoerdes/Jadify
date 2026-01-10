package io.jadify.core.scan;

import io.jadify.core.config.Config;
import io.jadify.core.model.ElementRef;

import java.util.List;
import java.util.Map;

public record ScanContext(
        Config config,
        List<ElementRef> publicApiElements,
        Map<ElementRef, String> docComments
) {
}
