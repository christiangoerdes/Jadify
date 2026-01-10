package io.jadify.cli;

import io.jadify.core.model.Issue;

import java.util.Comparator;
import java.util.List;

public final class ConsoleReporter {

    public void print(List<Issue> issues) {
        issues.stream()
                .sorted(Comparator.comparing((Issue i) -> i.element().sourceFile())
                        .thenComparing(i -> i.element().displayName()))
                .forEach(i -> System.out.printf(
                        "[%s] %s (%s) - %s%n",
                        i.severity(), i.message(), i.ruleId(), i.element().sourceFile()
                ));
        System.out.printf("Issues: %d%n", issues.size());
    }
}