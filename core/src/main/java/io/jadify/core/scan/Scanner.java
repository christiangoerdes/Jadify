package io.jadify.core.scan;

import io.jadify.core.config.Config;

import java.io.IOException;
import java.nio.file.Path;

public interface Scanner {
    ScanContext scan(Path projectRoot, Config config) throws IOException;
}
