package io.jadify.core.config;

import io.jadify.core.config.Config.FailOn;
import io.jadify.core.config.Config.RuleToggle;
import io.jadify.core.config.Config.Scope;

import java.util.List;

import static io.jadify.core.model.Severity.ERROR;

public final class ConfigDefaults {

    public static final Config DEFAULT = new Config(
            "src/main/java",
            List.of("**"),
            List.of("**.internal.**"),
            List.of(),
            new Scope(true, false),
            List.of(new RuleToggle("public-javadoc-presence", true)),
            new FailOn(ERROR)
    );

    private ConfigDefaults() {}

}
