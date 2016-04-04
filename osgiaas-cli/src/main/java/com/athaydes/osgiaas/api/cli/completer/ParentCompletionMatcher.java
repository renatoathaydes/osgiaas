package com.athaydes.osgiaas.api.cli.completer;

import java.util.List;

/**
 * A simple base class for {@link CompletionMatcher} which have children.
 */
public abstract class ParentCompletionMatcher implements CompletionMatcher {

    private final List<CompletionMatcher> children;

    public ParentCompletionMatcher( List<CompletionMatcher> children ) {
        this.children = children;
    }

    @Override
    public List<CompletionMatcher> children() {
        return children;
    }
}
