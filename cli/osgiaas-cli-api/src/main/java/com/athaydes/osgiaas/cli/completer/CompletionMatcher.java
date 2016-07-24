package com.athaydes.osgiaas.cli.completer;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
     * @return stream of children of this matcher. This method should return a different Stream each
     * time it is called, and the elements may be different each time.
     */
    Stream<CompletionMatcher> children();

    /**
     * @param command partially entered user command
     * @return true if this matcher can match the beginning of the given command, false otherwise.
     */
    boolean partiallyMatches( String command );

    /**
     * Creates a name {@link CompletionMatcher} which uses all of the given options to auto-complete.
     *
     * @param options alternative matchers
     * @return a {@link CompletionMatcher} that matches arguments by name.
     */
    static CompletionMatcher alternativeMatchers( CompletionMatcher... options ) {
        return new CompletionMatcherCollection( options );
    }

    /**
     * Creates a name {@link CompletionMatcher} which uses all of the given options to auto-complete.
     *
     * @param options alternative matchers
     * @return a {@link CompletionMatcher} that matches arguments by name.
     */
    static CompletionMatcher alternativeMatchers( Supplier<Stream<CompletionMatcher>> options ) {
        return new CompletionMatcherCollection( options );
    }

    /**
     * Creates a name {@link CompletionMatcher}.
     *
     * @param name     of arguments that match
     * @param children of this matcher
     * @return a {@link CompletionMatcher} that matches arguments by name.
     */
    static CompletionMatcher nameMatcher( String name, CompletionMatcher... children ) {
        return nameMatcher( name, () -> Stream.of( children ) );
    }

    /**
     * Creates a name {@link CompletionMatcher} with children.
     *
     * @param name     of arguments that match
     * @param children of this matcher
     * @return a {@link CompletionMatcher} that matches arguments by name.
     */
    static CompletionMatcher nameMatcher( String name, Supplier<Stream<CompletionMatcher>> children ) {
        if ( name == null || name.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Node name must be non-empty" );
        }
        return new NodeNameCompletionMatcher( name, children );
    }

    /**
     * Creates a multi-part {@link CompletionMatcher}.
     *
     * @param separator parts separator
     * @param parts     possible completions for each part. All parts must be childless because if there's any
     *                  completer after all parts are completed, it should be given as the children parameter.
     * @param children  of this matcher
     * @return a {@link CompletionMatcher} that matches arguments by using parts separated by the given separator.
     */
    static CompletionMatcher multiPartMatcher( String separator,
                                               List<CompletionMatcher> parts,
                                               Supplier<Stream<CompletionMatcher>> children ) {
        if ( separator == null || separator.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Separator must be non-empty" );
        }
        if ( parts.size() < 2 ) {
            throw new IllegalArgumentException( "There must be at least 2 parts, found only " +
                    parts.size() + " parts" );
        }

        return new MultiPartCompletionMatcher( separator, parts, children );
    }

}
