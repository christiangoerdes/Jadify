package io.jadify.core.scan;

import io.jadify.core.config.JadifyConfig;

import java.io.IOException;
import java.nio.file.Path;

public interface Scanner {
    ScanContext scan(Path projectRoot, JadifyConfig config) throws IOException;
}
