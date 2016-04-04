package com.athaydes.osgiaas.api.cli.completer;

import java.util.Collections;
import java.util.List;

/**
 * A matcher for parameter completion.
 * <p>
 * It can provide completions based on a partially-entered argument and determine whether a fully entered
 * argument can be matched against the possible completions.
 */
public interface CompletionMatcher {

    /**
     * @param argument user argument
     * @return true if a possible completion matches the given argument fully, false otherwise.
     */
    default boolean argumentFullyMatched( String argument ) {
        return completionsFor( argument ).contains( argument );
    }

    /**
     * @param argument partial user argument
     * @return all possible completions for the partially entered argument
     */
    List<String> completionsFor( String argument );

    /**
     * A {@link CompletionMatcher} should only match against an individual argument.
     * To match against a chain of arguments, a matcher may have children which match the next
     * argument in the chain, recursively.
     *
     * @return children of this matcher
     */
    List<CompletionMatcher> children();

    /**
     * @param command partially entered user command
     * @return true if this matcher can match the beginning of the given command, false otherwise.
     */
    boolean partiallyMatches( String command );

    /**
     * Creates a name {@link CompletionMatcher}.
     *
     * @param name of arguments that match
     * @return a {@link CompletionMatcher} that matches arguments by name.
     */
    static CompletionMatcher nameMatcher( String name ) {
        return nameMatcher( name, Collections.emptyList() );
    }

    static CompletionMatcher nameMatcher( String name, List<CompletionMatcher> children ) {
        if ( name == null || name.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Node name must be non-empty" );
        }
        return new NodeNameCompletionMatcher( name, children );
    }

}
