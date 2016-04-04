package com.athaydes.osgiaas.api.cli.completer;

import java.util.Collections;
import java.util.List;

class NodeNameCompletionMatcher implements CompletionMatcher {

    private final String name;
    private final List<CompletionMatcher> children;

    NodeNameCompletionMatcher( String name, List<CompletionMatcher> children ) {
        this.name = name;
        this.children = children;
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
    public List<CompletionMatcher> children() {
        return children;
    }

    @Override
    public boolean partiallyMatches( String prefix ) {
        // only allow matching if the prefix contains the full name and a whitespace
        // so we don't try to complete when the argument is already entered fully
        return prefix.startsWith( name + " " );
    }

}
