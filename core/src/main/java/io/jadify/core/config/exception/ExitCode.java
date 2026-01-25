package io.jadify.core.config.exception;

public enum ExitCode {

    SUCCESS(0),
    CONFIG(10),
    SCAN(20),
    RULE(30),
    FAILURE(40),
    UNEXPECTED(1);

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}