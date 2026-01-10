package io.jadify.core.rules;

import io.jadify.core.model.Issue;
import io.jadify.core.scan.ScanContext;

import java.util.List;

public interface Rule {
    String getName();
    List<Issue> evaluate(ScanContext ctx);
}
