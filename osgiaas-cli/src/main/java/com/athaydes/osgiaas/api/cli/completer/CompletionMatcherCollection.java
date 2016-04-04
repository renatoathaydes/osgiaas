package com.athaydes.osgiaas.api.cli.completer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CompletionMatcherCollection extends ParentCompletionMatcher
        implements Iterable<CompletionMatcher> {

    private final List<CompletionMatcher> matchers;

    public CompletionMatcherCollection( List<CompletionMatcher> matchers ) {
        this( matchers, Collections.emptyList() );
    }

    public CompletionMatcherCollection( List<CompletionMatcher> matchers,
                                        List<CompletionMatcher> children ) {
        super( children );
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
