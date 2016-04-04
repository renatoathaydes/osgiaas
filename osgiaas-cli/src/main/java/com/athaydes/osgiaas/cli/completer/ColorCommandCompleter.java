package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColorCommandCompleter extends BaseCompleter {

    private static final List<CompletionMatcher> colorTargets =
            Stream.of( "prompt", "text", "error" )
                    .map( CompletionMatcher::nameMatcher )
                    .collect( Collectors.toList() );

    private static final List<CompletionMatcher> colors = colorsNodesWithChildren( colorTargets );

    static List<CompletionMatcher> colorsNodesWithChildren(
            List<CompletionMatcher> children ) {
        return Stream.of( AnsiColor.values() )
                .map( AnsiColor::name )
                .filter( color -> !color.startsWith( "_" ) )
                .map( String::toLowerCase )
                .sorted()
                .map( color -> CompletionMatcher.nameMatcher( color, children ) )
                .collect( Collectors.toList() );
    }

    public ColorCommandCompleter() {
        super( CompletionMatcher.nameMatcher( "color", colors ) );
    }

}
