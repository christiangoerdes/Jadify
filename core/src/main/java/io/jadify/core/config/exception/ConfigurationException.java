package io.jadify.core.config.exception;

import static io.jadify.core.config.exception.ExitCode.CONFIG;

public class ConfigurationException extends JadifyException {

    public ConfigurationException(String message) {
        super(message, CONFIG);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, CONFIG, cause);
    }

}
