package com.athaydes.osgiaas.cli.completer;

import java.util.List;
import java.util.stream.Stream;

class AnyLevelMatcher implements CompletionMatcher {

    private final CompletionMatcher other;

    AnyLevelMatcher( CompletionMatcher other ) {
        this.other = other;
    }

    @Override
    public List<String> completionsFor( String argument ) {
        return other.completionsFor( argument );
    }

    @Override
    public Stream<CompletionMatcher> children() {
        return Stream.of( this );
    }

    @Override
    public boolean partiallyMatches( String command ) {
        return true;
    }

    @Override
    public boolean argumentFullyMatched( String argument ) {
        return true;
    }
}
