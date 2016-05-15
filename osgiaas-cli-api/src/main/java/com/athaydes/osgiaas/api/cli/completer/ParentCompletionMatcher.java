package com.athaydes.osgiaas.api.cli.completer;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A simple base class for {@link CompletionMatcher} which have children.
 */
public abstract class ParentCompletionMatcher implements CompletionMatcher {

    private final Supplier<Stream<CompletionMatcher>> children;

    public ParentCompletionMatcher( Supplier<Stream<CompletionMatcher>> children ) {
        this.children = children;
    }

    @Override
    public Stream<CompletionMatcher> children() {
        return children.get();
    }
}
