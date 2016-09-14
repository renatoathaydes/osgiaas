package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColorCommandCompleter extends BaseCompleter {

    private static final List<CompletionMatcher> colorTargets =
            Stream.of( "prompt", "text", "error" )
                    .map( CompletionMatcher::nameMatcher )
                    .collect( Collectors.toList() );

    private static final CompletionMatcher[] colors = colorsNodesWithChildren( colorTargets );

    static CompletionMatcher[] colorsNodesWithChildren(
            CompletionMatcher... children ) {
        return colorsNodesWithChildren( Arrays.asList( children ) );
    }

    static CompletionMatcher[] colorsNodesWithChildren(
            List<CompletionMatcher> children ) {
        Stream<CompletionMatcher> matcherStream = Stream.of( AnsiColor.values() )
                .map( AnsiColor::name )
                .filter( color -> !color.startsWith( "_" ) )
                .map( String::toLowerCase )
                .sorted()
                .map( color -> CompletionMatcher.nameMatcher( color, children::stream ) );

        return matcherStream.toArray( CompletionMatcher[]::new );
    }

    public ColorCommandCompleter() {
        super( CompletionMatcher.nameMatcher( "color", colors ) );
    }

}