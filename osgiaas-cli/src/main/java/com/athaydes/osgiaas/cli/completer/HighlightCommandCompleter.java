package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.command.HighlightCommand.BACKGROUND_ARG;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.FOREGROUND_ARG;

public class HighlightCommandCompleter extends BaseCompleter {

    private static final List<CompletionNode> colorNodes = ColorCommandCompleter
            .colorsNodes( CompletionNode.CompletionNodeBuilder::build );

    private static CompletionNode nodeForTopLevelArg( String arg ) {
        String nextArg = arg.equals( FOREGROUND_ARG ) ?
                BACKGROUND_ARG :
                FOREGROUND_ARG;

        return CompletionNode.nodeFor( arg )
                .withChildren( ColorCommandCompleter.colorsNodes( builder -> builder
                        .withChild( CompletionNode.nodeFor( nextArg )
                                .withChildren( colorNodes )
                                .build()
                        ).build() ) )
                .build();
    }

    public HighlightCommandCompleter() {
        super( CompletionNode.nodeFor( "highlight" )
                .withChildren( Stream.of( FOREGROUND_ARG, BACKGROUND_ARG )
                        .map( HighlightCommandCompleter::nodeForTopLevelArg )
                        .collect( Collectors.toList() ) )
                .build() );
    }

}
