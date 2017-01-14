package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.core.command.ColorCommand;

import java.util.stream.Stream;

public class ColorCommandCompleter extends BaseCompleter {

    private static final CompletionMatcher completionMatcher;

    static {
        CompletionMatcher[] colorMatchers = AnsiColor.colorNames().stream()
                .map( it -> CompletionMatcher.nameMatcher( it.toLowerCase() ) )
                .toArray( CompletionMatcher[]::new );

        completionMatcher = CompletionMatcher.alternativeMatchers(
                Stream.of( ColorCommand.ColorTarget.values() )
                        .map( it -> CompletionMatcher.nameMatcher( it.name().toLowerCase(),
                                CompletionMatcher.alternativeMatchers( colorMatchers ) ) )
                        .toArray( CompletionMatcher[]::new ) );
    }

    public ColorCommandCompleter() {
        super( CompletionMatcher.nameMatcher( "color", completionMatcher ) );
    }

}
