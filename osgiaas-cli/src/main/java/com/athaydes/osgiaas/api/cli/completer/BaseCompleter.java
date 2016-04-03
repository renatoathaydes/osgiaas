package com.athaydes.osgiaas.api.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.CommandHelper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A base class which makes it very simple to implement a {@link CommandCompleter}.
 */
public class BaseCompleter implements CommandCompleter {

    private final CompletionNode rootNode;
    private final String completionRoot;

    public BaseCompleter( CompletionNode completionNode ) {
        this.rootNode = completionNode;
        this.completionRoot = completionNode.name() + " ";
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        if ( buffer.startsWith( completionRoot ) && cursor >= completionRoot.length() ) {
            String prefix = buffer.substring( 0, cursor );
            List<String> parts = CommandHelper.breakupArguments( prefix );
            if ( prefix.endsWith( " " ) ) {
                parts.add( "" );
            }

            List<String> completions = completionsFor( rootNode, dropFirst( parts ) );

            candidates.addAll( completions );
            if ( !completions.isEmpty() ) {
                return prefix.lastIndexOf( ' ' ) + 1;
            }
        }
        return -1;
    }

    private static List<String> completionsFor( CompletionNode node,
                                                List<String> parts ) {
        if ( parts.isEmpty() ) {
            return Collections.emptyList();
        } else if ( parts.size() == 1 ) {
            String prefix = parts.get( 0 );
            return node.children().stream()
                    .map( CompletionNode::name )
                    .filter( name -> name.startsWith( prefix ) )
                    .collect( Collectors.toList() );
        } else {
            String part = parts.get( 0 );
            Optional<CompletionNode> nextNode = node.children().stream()
                    .filter( child -> child.name().equals( part ) )
                    .findFirst();

            if ( nextNode.isPresent() ) {
                return completionsFor( nextNode.get(), dropFirst( parts ) );
            } else {
                return Collections.emptyList();
            }
        }
    }

    private static List<String> dropFirst( List<String> parts ) {
        return parts.stream().skip( 1 ).collect( Collectors.toList() );
    }

}
