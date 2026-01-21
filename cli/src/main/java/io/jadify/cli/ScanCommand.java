package io.jadify.cli;

import io.jadify.core.JadifyRunner;
import io.jadify.core.config.ConfigLoader;
import io.jadify.core.model.Severity;
import io.jadify.core.rules.PublicJavadocPresenceRule;
import io.jadify.core.scan.JavaSourceScanner;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(name = "scan", mixinStandardHelpOptions = true)
public final class ScanCommand implements Runnable {

    @CommandLine.Option(names = {"-c", "--config"}, required = true)
    Path configFile;

    @CommandLine.Parameters(index = "0", description = "Project root (e.g. src/main/java)")
    Path projectRoot;

    @Override
    public void run() {
        try {
            var config = ConfigLoader.load(configFile);

            var runner = new JadifyRunner(
                    new JavaSourceScanner(),
                    List.of(new PublicJavadocPresenceRule())
            );

            var issues = runner.run(projectRoot, config);
            new ConsoleReporter().print(issues);

            Severity failAt = config.failOn().severity();
            boolean shouldFail = issues.stream().anyMatch(i -> i.severity().ordinal() >= failAt.ordinal());
            if (shouldFail) throw new RuntimeException();

        } catch (Exception e) {
            System.err.println("Jadify failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
