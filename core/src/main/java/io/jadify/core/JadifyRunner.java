package io.jadify.core;

import io.jadify.core.config.Config;
import io.jadify.core.model.Issue;
import io.jadify.core.rules.Rule;
import io.jadify.core.scan.Scanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JadifyRunner {

    private final Scanner scanner;
    private final List<Rule> rules;

    public JadifyRunner(Scanner scanner, List<Rule> rules) {
        this.scanner = scanner;
        this.rules = rules;
    }

    public List<Issue> run(Path projectRoot, Config config) throws Exception {
        var ctx = scanner.scan(projectRoot, config);
        var issues = new ArrayList<Issue>();
        for (Rule rule : rules) {
            issues.addAll(rule.evaluate(ctx));
        }
        return issues;
    }
}
