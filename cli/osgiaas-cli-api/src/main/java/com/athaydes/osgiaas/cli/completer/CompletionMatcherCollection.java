package com.athaydes.osgiaas.cli.completer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    private final Supplier<Stream<CompletionMatcher>> matchers;

    public CompletionMatcherCollection( CompletionMatcher... matchers ) {
        this( () -> Stream.of( matchers ) );
    }

    public CompletionMatcherCollection( Supplier<Stream<CompletionMatcher>> matchers ) {
        super( () -> matchers.get().flatMap( CompletionMatcher::children ) );
        this.matchers = matchers;
    }

    @Override
    public List<String> completionsFor( String argument ) {
        List<String> result = new ArrayList<>();
        matchers.get().forEach( matcher -> result.addAll( matcher.completionsFor( argument ) ) );
        return result;
    }

    @Override
    public boolean partiallyMatches( String command ) {
        return matchers.get().anyMatch( m -> m.partiallyMatches( command ) );
    }

    @Override
    public Iterator<CompletionMatcher> iterator() {
        return matchers.get().iterator();
    }

    @Override
    public String toString() {
        return "CompletionMatcherCollection{" +
                "matchers=" + matchers +
                '}';
    }
}
