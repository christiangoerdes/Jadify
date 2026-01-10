package io.jadify.core.rules;

import io.jadify.core.model.Issue;
import io.jadify.core.model.Severity;
import io.jadify.core.scan.ScanContext;

import java.util.ArrayList;
import java.util.List;

public class PublicJavadocPresenceRule implements Rule {

    @Override
    public String getName() {
        return "public-javadoc-presence";
    }

    @Override
    public List<Issue> evaluate(ScanContext ctx) {
        var issues = new ArrayList<Issue>();
        for (var el : ctx.publicApiElements()) {
            String doc = ctx.docComments().get(el);
            if (doc == null || doc.trim().isEmpty()) {
                issues.add(new Issue(
                        Severity.ERROR,
                        getName(),
                        "Missing Javadoc: " + el.displayName(),
                        el
                ));
            }
        }
        return issues;
    }
}
