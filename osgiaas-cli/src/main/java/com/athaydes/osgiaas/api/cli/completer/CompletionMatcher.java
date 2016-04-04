package com.athaydes.osgiaas.api.cli.completer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Creates a name {@link CompletionMatcher} with children.
     *
     * @param name     of arguments that match
     * @param children of this matcher
     * @return a {@link CompletionMatcher} that matches arguments by name.
     */
    static CompletionMatcher nameMatcher( String name, List<CompletionMatcher> children ) {
        if ( name == null || name.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Node name must be non-empty" );
        }
        return new NodeNameCompletionMatcher( name, children );
    }

    /**
     * Creates a multi-part {@link CompletionMatcher}.
     *
     * @param separator parts separator
     * @param parts     possible completions for each part
     * @return a {@link CompletionMatcher} that matches arguments by using parts separated by the given separator.
     */
    static CompletionMatcher multiPartMatcher( String separator, List<List<String>> parts ) {
        return multiPartMatcher( separator, parts, Collections.emptyList() );
    }

    /**
     * Creates a multi-part {@link CompletionMatcher} with children.
     *
     * @param separator parts separator
     * @param parts     possible completions for each part
     * @param children  of this matcher
     * @return a {@link CompletionMatcher} that matches arguments by using parts separated by the given separator.
     */
    static CompletionMatcher multiPartMatcher( String separator,
                                               List<List<String>> parts,
                                               List<CompletionMatcher> children ) {
        if ( separator == null || separator.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Separator must be non-empty" );
        }

        List<CompletionMatcherCollection> matchers = parts.stream()
                .map( part -> new CompletionMatcherCollection( part.stream()
                        .map( CompletionMatcher::nameMatcher )
                        .collect( Collectors.toList() ) ) )
                .collect( Collectors.toList() );

        return new MultiPartCompletionMatcher( separator, matchers, children );
    }

}
