package io.jadify.core.config;

import com.networknt.schema.*;
import com.networknt.schema.Error;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Path;
import java.util.List;

import static com.networknt.schema.SchemaLocation.of;
import static com.networknt.schema.SchemaRegistry.withDefaultDialect;
import static com.networknt.schema.SpecificationVersion.DRAFT_2020_12;
import static io.jadify.core.config.ConfigDefaults.DEFAULT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;

public final class ConfigLoader {

    private static final YAMLMapper YAML = YAMLMapper.builder().build();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final SchemaRegistry REGISTRY = withDefaultDialect(DRAFT_2020_12);

    private static final Schema SCHEMA = REGISTRY.getSchema(of("classpath:jadify-schema/jadify-config.schema.json"));

    public static Config load(Path yamlFile) throws Exception {
        return load(yamlFile, DEFAULT);
    }

    public static Config load(Path yamlFile, Config defaultConfig) throws Exception {
        ObjectNode defaults = JSON.valueToTree(defaultConfig);
        deepMerge(defaults, YAML.readTree(readString(yamlFile, UTF_8)));

        List<Error> errors = SCHEMA.validate(JSON.writeValueAsString(defaults), InputFormat.JSON, ctx -> {});
        if (!errors.isEmpty()) throw new ConfigValidationException(errors);

        return JSON.treeToValue(defaults, Config.class);
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
