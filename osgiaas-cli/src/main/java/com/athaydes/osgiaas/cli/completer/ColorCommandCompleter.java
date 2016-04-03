package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColorCommandCompleter extends BaseCompleter {

    private static final List<CompletionNode> colorTargets =
            Stream.of( "prompt", "text", "error" )
                    .map( target -> CompletionNode.nodeFor( target ).build() )
                    .collect( Collectors.toList() );

    private static final List<CompletionNode> colors =
            Stream.of( AnsiColor.values() )
                    .map( AnsiColor::name )
                    .filter( color -> !color.startsWith( "_" ) )
                    .map( String::toLowerCase )
                    .sorted()
                    .map( color -> CompletionNode.nodeFor( color )
                            .withChildren( colorTargets )
                            .build() )
                    .collect( Collectors.toList() );

    public ColorCommandCompleter() {
        super( CompletionNode.nodeFor( "color" )
                .withChildren( colors )
                .build() );
    }

}
