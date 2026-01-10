package io.jadify.core.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class ConfigLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private ConfigLoader() {}

    public static JadifyConfig load(Path path) throws IOException {
        return MAPPER.readValue(path.toFile(), JadifyConfig.class);
    }
}