package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.command.HighlightCommand.BACKGROUND_ARG;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.FOREGROUND_ARG;

public class HighlightCommandCompleter extends BaseCompleter {

    private static final List<CompletionMatcher> colorNodes = ColorCommandCompleter
            .colorsNodesWithChildren( Collections.emptyList() );

    private static CompletionMatcher nodeForTopLevelArg( String arg ) {
        String nextArg = arg.equals( FOREGROUND_ARG ) ?
                BACKGROUND_ARG :
                FOREGROUND_ARG;

        return CompletionMatcher.nameMatcher( arg,
                ColorCommandCompleter.colorsNodesWithChildren( Collections.singletonList(
                        CompletionMatcher.nameMatcher( nextArg, colorNodes ) ) ) );
    }

    public HighlightCommandCompleter() {
        super( CompletionMatcher.nameMatcher( "highlight",
                Stream.of( FOREGROUND_ARG, BACKGROUND_ARG )
                        .map( HighlightCommandCompleter::nodeForTopLevelArg )
                        .collect( Collectors.toList() ) ) );
    }

}
