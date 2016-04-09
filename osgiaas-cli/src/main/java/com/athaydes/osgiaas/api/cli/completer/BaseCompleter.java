package com.athaydes.osgiaas.api.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.CommandHelper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A base class which makes it very simple to implement a {@link CommandCompleter}.
 */
public class BaseCompleter implements CommandCompleter {

    private final CompletionMatcher rootNode;

    public BaseCompleter( CompletionMatcher completionNode ) {
        this.rootNode = completionNode;
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        String prefix = buffer.substring( 0, cursor );

        if ( rootNode.partiallyMatches( prefix ) ) {
            List<String> parts = new LinkedList<>( CommandHelper.breakupArguments( prefix ) );
            if ( prefix.endsWith( " " ) ) {
                parts.add( "" );
            }

            List<String> completions = completionsFor( rootNode, dropFirst( parts ) );

            candidates.addAll( completions );
            if ( !completions.isEmpty() ) {
                return CommandHelper.lastSeparatorIndex( prefix ) + 1;
            }
        }
        return -1;
    }

    private static List<String> completionsFor( CompletionMatcher node,
                                                List<String> parts ) {
        if ( parts.isEmpty() ) {
            return Collections.emptyList();
        } else if ( parts.size() == 1 ) {
            // try to complete the last part
            String prefix = parts.get( 0 );
            return node.children().stream()
                    .flatMap( child -> child.completionsFor( prefix ).stream() )
                    .collect( Collectors.toList() );
        } else {
            // check if the next part matches one of the possible completions and continue on to the next part if so
            String part = parts.get( 0 );
            Optional<CompletionMatcher> nextNode = node.children().stream()
                    .filter( child -> child.argumentFullyMatched( part ) )
                    .findFirst();

            if ( nextNode.isPresent() ) {
                return completionsFor( nextNode.get(), dropFirst( parts ) );
            } else {
                return Collections.emptyList();
            }
        }
    }

    private static List<String> dropFirst( List<String> parts ) {
        parts.remove( 0 );
        return parts;
    }

}
