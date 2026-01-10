package io.jadify.core.config.schema;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.jadify.core.config.Config;

public class JsonSchemaGenerator {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {
        Path outDir = (args.length >= 1) ? Path.of(args[0]) : Path.of("target/generated-schemas");
        Files.createDirectories(outDir);

        MAPPER.writerWithDefaultPrettyPrinter().writeValue(
                outDir.resolve((args.length >= 2) ? args[1] : "jadify-config.schema.json").toFile(),
                generateSchema()
        );
    }

    public static ObjectNode generateSchema() {
        return new Generator().generate(Config.class);
    }

    private static final class Generator {

        private final Map<String, ObjectNode> defs = new LinkedHashMap<>();

        ObjectNode generate(@SuppressWarnings("SameParameterValue") Class<?> rootType) {
            ensureDefinition(rootType);

            ObjectNode root = MAPPER.createObjectNode();
            root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
            root.put("title", rootType.getSimpleName());
            root.put("$ref", "#/$defs/" + defName(rootType));

            ObjectNode defsNode = MAPPER.createObjectNode();
            defs.forEach(defsNode::set);
            root.set("$defs", defsNode);

            return root;
        }

        private JsonNode schemaFor(Type type) {
            if (type instanceof Class<?> cls) {
                return schemaForClass(cls);
            }
            if (type instanceof ParameterizedType pt) {
                if (pt.getRawType() instanceof Class<?> rawCls) {
                    if (List.class.isAssignableFrom(rawCls)) {
                        ObjectNode n = MAPPER.createObjectNode();
                        n.put("type", "array");
                        n.set("items", schemaFor(pt.getActualTypeArguments()[0]));
                        return n;
                    }
                    // fall back to raw type
                    return schemaForClass(rawCls);
                }
            }
            if (type instanceof GenericArrayType gat) {
                ObjectNode n = MAPPER.createObjectNode();
                n.put("type", "array");
                n.set("items", schemaFor(gat.getGenericComponentType()));
                return n;
            }
            throw new IllegalArgumentException("Unsupported type: " + type);
        }

        private JsonNode schemaForClass(Class<?> cls) {
            if (cls.isArray()) {
                ObjectNode n = MAPPER.createObjectNode();
                n.put("type", "array");
                n.set("items", schemaFor(cls.getComponentType()));
                return n;
            }
            if (cls.isEnum()) {
                ObjectNode n = MAPPER.createObjectNode();
                n.put("type", "string");
                ArrayNode e = MAPPER.createArrayNode();
                for (Object c : cls.getEnumConstants()) {
                    e.add(((Enum<?>) c).name());
                }
                n.set("enum", e);
                return n;
            }
            if (isString(cls)) return typeNode("string");
            if (isBoolean(cls)) return typeNode("boolean");
            if (isInteger(cls)) return typeNode("integer");
            if (isNumber(cls)) return typeNode("number");

            if (cls.isRecord()) {
                ensureDefinition(cls);
                ObjectNode ref = MAPPER.createObjectNode();
                ref.put("$ref", "#/$defs/" + defName(cls));
                return ref;
            }

            throw new IllegalArgumentException("Unsupported class (only record/enum/primitives/List supported): " + cls.getName());
        }

        private void ensureDefinition(Class<?> recordType) {
            if (!recordType.isRecord()) return;

            String name = defName(recordType);
            if (defs.containsKey(name)) return;

            ObjectNode def = MAPPER.createObjectNode();
            def.put("type", "object");
            def.put("additionalProperties", false);

            ObjectNode props = MAPPER.createObjectNode();
            ArrayNode required = MAPPER.createArrayNode();

            for (RecordComponent rc : recordType.getRecordComponents()) {
                props.set(rc.getName(), schemaFor(rc.getGenericType()));
                required.add(rc.getName());
            }

            def.set("properties", props);
            def.set("required", required);

            defs.put(name, def);
        }

        private static ObjectNode typeNode(String t) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("type", t);
            return n;
        }

        private static String defName(Class<?> cls) {
            return cls.getName().replace('.', '_').replace('$', '_');
        }

        private static boolean isString(Class<?> c) {
            return c == String.class || c == char.class || c == Character.class || CharSequence.class.isAssignableFrom(c);
        }

        private static boolean isBoolean(Class<?> c) {
            return c == boolean.class || c == Boolean.class;
        }

        private static boolean isInteger(Class<?> c) {
            return c == byte.class || c == Byte.class
                    || c == short.class || c == Short.class
                    || c == int.class || c == Integer.class
                    || c == long.class || c == Long.class;
        }

        private static boolean isNumber(Class<?> c) {
            return c == float.class || c == Float.class
                    || c == double.class || c == Double.class
                    || Number.class.isAssignableFrom(c);
        }
    }
}
