package io.jadify.core;

import io.jadify.core.config.JadifyConfig;
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

    public List<Issue> run(Path projectRoot, JadifyConfig config) throws Exception {
        var ctx = scanner.scan(projectRoot, config);

        var enabled = config.rules().stream()
                .filter(JadifyConfig.RuleToggle::enabled)
                .map(JadifyConfig.RuleToggle::id)
                .collect(java.util.stream.Collectors.toSet());

        List<Issue> all = new ArrayList<>();
        for (var r : rules) {
            if (enabled.contains(r.getName())) {
                all.addAll(r.evaluate(ctx));
            }
        }
        return all;
    }
}
