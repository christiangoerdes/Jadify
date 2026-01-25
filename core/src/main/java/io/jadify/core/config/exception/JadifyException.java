package io.jadify.core.config.exception;

public class JadifyException extends RuntimeException {

    private final ExitCode exitCode;

    protected JadifyException(String message, ExitCode exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    protected JadifyException(String message, ExitCode exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode exitCode() {
        return exitCode;
    }

}
