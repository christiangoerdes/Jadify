package io.jadify.core.config;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.networknt.schema.SpecificationVersion.DRAFT_2020_12;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class ConfigLoader {

    private static final YAMLMapper YAML = new YAMLMapper();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Schema SCHEMA = loadSchemaFromClasspath();

    public static JadifyConfig load(Path yamlFile) throws Exception {
        JsonNode node = YAML.readTree(Files.readString(yamlFile, UTF_8));

        List<Error> errors = SCHEMA.validate(JSON.writeValueAsString(node), InputFormat.JSON);
        if (!errors.isEmpty()) throw new ConfigValidationException(errors);

        return YAML.treeToValue(node, JadifyConfig.class);
    }

    private static Schema loadSchemaFromClasspath() {
        String resource = "/jadify-schema/jadify-config.schema.json";

        try (InputStream is = ConfigLoader.class.getResourceAsStream(resource)) {
            if (is == null) throw new IllegalStateException("Missing schema resource: " + resource);

            return SchemaRegistry.withDefaultDialect(DRAFT_2020_12).getSchema(new String(is.readAllBytes(), UTF_8), InputFormat.JSON);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load/compile schema from classpath", e);
        }
    }

    private ConfigLoader() {}

    public static final class ConfigValidationException extends IllegalArgumentException {
        private final List<Error> errors;

        public ConfigValidationException(List<Error> errors) {
            super(format(errors));
            this.errors = List.copyOf(errors);
        }

        public List<Error> errors() { return errors; }

        private static String format(List<Error> errors) {
            StringBuilder sb = new StringBuilder("Config does not match schema:\n");
            for (Error e : errors) {
                sb.append("- ")
                        .append(e.getInstanceLocation()).append(" ")
                        .append(e.getKeyword()).append(": ")
                        .append(e.getMessage()).append("\n");
            }
            return sb.toString();
        }
    }
}
