package io.jadify.core;

import io.jadify.core.config.ConfigLoader;
import io.jadify.core.model.Issue;
import io.jadify.core.model.Severity;
import io.jadify.core.rules.PublicJavadocPresenceRule;
import io.jadify.core.scan.JavaSourceScanner;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class Starter {

    public static void main(String[] args) throws Exception {
        Path projectRoot = args.length > 0 ? Path.of(args[0]) : Path.of("core");
        Path configFile = args.length > 1 ? Path.of(args[1]) : null;

        var config = ConfigLoader.load(configFile);
        var runner = new JadifyRunner(
                new JavaSourceScanner(),
                List.of(new PublicJavadocPresenceRule())
        );

        var issues = runner.run(projectRoot, config);
        printIssues(issues);

        Severity failAt = config.failOn().severity();
        boolean shouldFail = issues.stream().anyMatch(i -> i.severity().ordinal() >= failAt.ordinal());
        if (shouldFail) {
            throw new IllegalStateException("Failing build due to severity >= " + failAt);
        }
    }

    // TODO same as in cli. Move to Util
    private static void printIssues(List<Issue> issues) {
        issues.stream()
                .sorted(Comparator.comparing((Issue i) -> i.element().sourceFile())
                        .thenComparing(i -> i.element().displayName()))
                .forEach(i -> System.out.printf(
                        "[%s] %s (%s) - %s%n",
                        i.severity(), i.message(), i.ruleId(), i.element().sourceFile()
                ));
        System.out.printf("Issues: %d%n", issues.size());
    }

    private Starter() {}
}
