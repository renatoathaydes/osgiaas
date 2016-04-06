package com.athaydes.osgiaas.api.cli.completer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows handling multiple {@link CompletionMatcher}s as a single entity.
 * <p>
 * All CompletionMatchers within a {@link CompletionMatcherCollection} that can provide completions for some input
 * will be used to provide completions.
 * <p>
 * The children of a {@link CompletionMatcherCollection} are the sum of all its matchers' children.
 */
public class CompletionMatcherCollection extends ParentCompletionMatcher
        implements Iterable<CompletionMatcher> {

    private final List<CompletionMatcher> matchers;

    public CompletionMatcherCollection( List<CompletionMatcher> matchers ) {
        super( matchers.stream()
                .flatMap( m -> m.children().stream() )
                .collect( Collectors.toList() ) );
        this.matchers = matchers;
    }

    @Override
    public List<String> completionsFor( String argument ) {
        List<String> result = new ArrayList<>();
        for (CompletionMatcher matcher : matchers) {
            result.addAll( matcher.completionsFor( argument ) );
        }
        return result;
    }

    @Override
    public boolean partiallyMatches( String command ) {
        return matchers.stream().anyMatch( m -> m.partiallyMatches( command ) );
    }

    @Override
    public Iterator<CompletionMatcher> iterator() {
        return matchers.iterator();
    }

}
