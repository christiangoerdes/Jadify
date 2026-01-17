package io.jadify.core.config;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static com.networknt.schema.SchemaRegistry.withDefaultDialect;
import static com.networknt.schema.SpecificationVersion.DRAFT_2020_12;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;

/**
 * Loads YAML configuration with defaults and validates against the JSON schema.
 */
public final class ConfigLoader {

    private static final String DEFAULT_YAML_RESOURCE = "/jadify-config.default.yaml";

    private static final YAMLMapper YAML = YAMLMapper.builder().build();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Schema SCHEMA = withDefaultDialect(DRAFT_2020_12).getSchema(SchemaLocation.of("classpath:jadify-schema/jadify-config.schema.json"));

    public static Config load(Path yamlFile) throws Exception {
        if (yamlFile == null) {
          return loadDefault();
        }
        return load(yamlFile, DEFAULT_YAML_RESOURCE);
    }

    /** Load defaults from classpath YAML, then overlay the provided YAML file (missing fields stay at defaults). */
    public static Config load(Path yamlFile, String defaultYamlResource) throws Exception {
        ObjectNode merged = loadDefaultNode(defaultYamlResource);

        if (yamlFile != null) {
            deepMerge(merged, YAML.readTree(readString(yamlFile, UTF_8)));
        }

        List<Error> errors = SCHEMA.validate(JSON.writeValueAsString(merged), InputFormat.JSON, ctx -> {});
        if (!errors.isEmpty()) throw new ConfigValidationException(errors);

        return JSON.treeToValue(merged, Config.class);
    }

    /** Load defaults only (no overlay). */
    public static Config loadDefault() throws Exception {
        ObjectNode defaults = loadDefaultNode(DEFAULT_YAML_RESOURCE);

        List<Error> errors = SCHEMA.validate(JSON.writeValueAsString(defaults), InputFormat.JSON, ctx -> {});
        if (!errors.isEmpty()) throw new ConfigValidationException(errors);

        return JSON.treeToValue(defaults, Config.class);
    }

    private static ObjectNode loadDefaultNode(String resource) throws Exception {
        JsonNode n = readClasspathYaml(resource);
        if (!(n instanceof ObjectNode obj)) {
            throw new IllegalStateException("Default config must be a YAML object at root: " + resource);
        }
        return obj;
    }

    private static JsonNode readClasspathYaml(String resource) throws Exception {
        String normalized = resource.startsWith("/") ? resource : "/" + resource;
        try (InputStream is = ConfigLoader.class.getResourceAsStream(normalized)) {
            if (is == null) throw new IllegalStateException("Missing default config resource on classpath: " + normalized);
            return YAML.readTree(is);
        }
    }

    private static void deepMerge(ObjectNode base, JsonNode override) {
        if (override == null || override.isNull() || !override.isObject()) return;

        override.properties().iterator().forEachRemaining(e -> {
            String key = e.getKey();
            JsonNode oVal = e.getValue();
            JsonNode bVal = base.get(key);

            if (bVal != null && bVal.isObject() && oVal != null && oVal.isObject()) {
                deepMerge((ObjectNode) bVal, oVal);
            } else {
                base.set(key, oVal);
            }
        });
    }

    public static final class ConfigValidationException extends IllegalArgumentException {
        private final List<Error> errors;

        public ConfigValidationException(List<Error> errors) {
            super(format(errors));
            this.errors = List.copyOf(errors);
        }

        public List<Error> errors() { return errors; }

        private static String format(List<Error> errors) {
            StringBuilder sb = new StringBuilder("Config does not match schema:\n");
            for (Error e : errors) sb.append("- ").append(e).append('\n');
            return sb.toString();
        }
    }

    private ConfigLoader() {}
}
