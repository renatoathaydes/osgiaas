package com.athaydes.osgiaas.cli.completer;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

class NodeNameCompletionMatcher extends ParentCompletionMatcher {

    private final String name;

    NodeNameCompletionMatcher( String name, Supplier<Stream<CompletionMatcher>> children ) {
        super( children );
        this.name = name;
    }

    @Override
    public boolean argumentFullyMatched( String argument ) {
        return argument.equals( name );
    }

    @Override
    public List<String> completionsFor( String argument ) {
        return name.startsWith( argument ) ?
                Collections.singletonList( name ) :
                Collections.emptyList();
    }

    @Override
    public boolean partiallyMatches( String prefix ) {
        // only allow matching if the prefix contains the full name and a whitespace
        // so we don't try to complete when the argument is already entered fully
        return prefix.startsWith( name + " " );
    }

    @Override
    public String toString() {
        return "NodeNameCompletionMatcher{" +
                "name='" + name + '\'' +
                '}';
    }
}
