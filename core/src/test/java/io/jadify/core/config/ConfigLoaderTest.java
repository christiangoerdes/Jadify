package io.jadify.core.config;

import io.jadify.core.config.ConfigLoader.ConfigValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static io.jadify.core.config.ConfigLoader.load;
import static io.jadify.core.config.ConfigLoader.loadDefault;
import static io.jadify.core.model.Severity.ERROR;
import static io.jadify.core.model.Severity.WARN;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path tempDir;

    @Test
    void loadDefaultReadsClasspathConfig() throws Exception {
        Config config = loadDefault();

        assertEquals("src/main/java", config.projectRoot());
        assertEquals(ERROR, config.failOn().severity());
        assertFalse(config.scan().types().include().annotations());
    }

    @Test
    void loadNullPathDelegatesToDefault() throws Exception {
        assertEquals(loadDefault(), load((Path) null));
    }

    @Test
    void loadCustomDefaultResourceNormalizesPaths() throws Exception {
        assertEquals("src/main/java", load(null, "jadify-config.default.yaml").projectRoot());
    }

    @Test
    void loadMergesOverridesFromFile() throws Exception {
        Config config = load(writeYaml("""
                failOn:
                  severity: WARN
                scan:
                  members:
                    includeInherited: true
                """));
        assertEquals(WARN, config.failOn().severity());
        assertTrue(config.scan().members().includeInherited());
        assertFalse(config.scan().types().include().annotations());
    }

    @Test
    void loadThrowsConfigValidationExceptionWithErrors() {
        ConfigValidationException ex = assertThrows(
                ConfigValidationException.class,
                () -> load(writeYaml("""
                        projectRoot: 123
                        """))
        );

        assertTrue(ex.getMessage().startsWith("Config does not match schema:"));
        assertFalse(ex.errors().isEmpty());
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> ex.errors().add(null));
    }

    @Test
    void loadDefaultNodeRejectsNonObjectYaml() throws Exception {
        Method method = ConfigLoader.class.getDeclaredMethod("loadDefaultNode", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "/" + writeClasspathResource("test-configs/non-object-inline.yaml", """
                        - one
                        - two
                        """))
        );

        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Default config must be a YAML object"));
    }

    @Test
    void readClasspathYamlRejectsMissingResource() throws Exception {
        Method method = ConfigLoader.class.getDeclaredMethod("readClasspathYaml", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "missing.yaml")
        );

        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Missing default config resource"));
    }

    @Test
    void deepMergeOverwritesAndKeepsDefaults() throws Exception {
        ObjectNode base = JSON.createObjectNode();
        ObjectNode nested = JSON.createObjectNode();
        nested.put("inner", 1);
        base.set("nested", nested);
        base.put("keep", "yes");

        ObjectNode override = JSON.createObjectNode();
        ObjectNode nestedOverride = JSON.createObjectNode();
        nestedOverride.put("inner", 2);
        nestedOverride.put("added", 3);
        override.set("nested", nestedOverride);
        override.put("replaced", "value");

        invokeDeepMerge(base, override);

        assertEquals(2, base.path("nested").path("inner").asInt());
        assertEquals(3, base.path("nested").path("added").asInt());
        assertEquals("yes", base.path("keep").asString());
        assertEquals("value", base.path("replaced").asString());
    }

    @Test
    void deepMergeIgnoresNonObjectOverrides() throws Exception {
        ObjectNode base = JSON.createObjectNode();
        base.put("value", "keep");

        invokeDeepMerge(base, JsonNodeFactory.instance.stringNode("override"));
        invokeDeepMerge(base, null);

        assertEquals("keep", base.path("value").asString());
    }

    @Test
    void privateConstructorIsAccessibleForCoverage() throws Exception {
        Constructor<ConfigLoader> ctor = ConfigLoader.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        assertNotNull(ctor.newInstance());
    }

    private Path writeYaml(String content) throws IOException {
        Path yaml = tempDir.resolve("config.yaml");
        Files.writeString(yaml, content);
        return yaml;
    }

    private static void invokeDeepMerge(ObjectNode base, JsonNode override) throws Exception {
        Method method = ConfigLoader.class.getDeclaredMethod("deepMerge", ObjectNode.class, JsonNode.class);
        method.setAccessible(true);
        method.invoke(null, base, override);
    }

    @SuppressWarnings("SameParameterValue")
    private static String writeClasspathResource(String relativePath, String content) throws Exception {
        Path classpathRoot = Path.of(Objects.requireNonNull(ConfigLoaderTest.class.getResource("/")).toURI());
        Path target = classpathRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, CREATE, TRUNCATE_EXISTING);
        return relativePath;
    }
}
