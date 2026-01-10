package io.jadify.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "jadify",
        subcommands = { ScanCommand.class },
        mixinStandardHelpOptions = true
)
public final class JadifyMain implements Runnable {
    @Override public void run() { CommandLine.usage(this, System.out); }

    public static void main(String[] args) {
        int code = new CommandLine(new JadifyMain()).execute(args);
        System.exit(code);
    }
}
