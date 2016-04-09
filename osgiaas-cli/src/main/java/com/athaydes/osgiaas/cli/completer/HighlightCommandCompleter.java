package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.command.HighlightCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.multiPartMatcher;
import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.nameMatcher;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.BACKGROUND_ARG;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.FOREGROUND_ARG;
import static com.athaydes.osgiaas.cli.completer.ColorCommandCompleter.colorsNodesWithChildren;

public class HighlightCommandCompleter extends BaseCompleter {

    static CompletionMatcher ansiModifierNodesWithChildren(
            CompletionMatcher... children ) {
        return ansiModifierNodesWithChildren( Arrays.asList( children ) );
    }

    static CompletionMatcher ansiModifierNodesWithChildren(
            List<CompletionMatcher> children ) {
        Set<String> shortFormAnsiModifiers = HighlightCommand.ansiModifierNameByShortOption.keySet();

        Set<String> longFormAnsiModifiers = HighlightCommand.ansiModifierNameByShortOption.values().stream()
                .map( String::toLowerCase )
                .collect( Collectors.toSet() );

        return multiPartMatcher( "+", Arrays.asList(
                colorsNodesWithChildren(),
                flatten( Arrays.asList(
                        asMatchers( shortFormAnsiModifiers ),
                        asMatchers( longFormAnsiModifiers )
                ) )
        ), children );
    }

    private static List<CompletionMatcher> asMatchers( Set<String> shortFormAnsiModifiers ) {
        return shortFormAnsiModifiers.stream()
                .sorted()
                .map( CompletionMatcher::nameMatcher )
                .collect( Collectors.toList() );
    }

    private static List<CompletionMatcher> flatten( List<List<CompletionMatcher>> matchers ) {
        return matchers.stream()
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
    }

    public HighlightCommandCompleter() {
        super( nameMatcher( "highlight",
                nameMatcher( BACKGROUND_ARG, colorsNodesWithChildren(
                        nameMatcher( FOREGROUND_ARG,
                                ansiModifierNodesWithChildren() )
                ) ),
                nameMatcher( FOREGROUND_ARG, ansiModifierNodesWithChildren(
                        nameMatcher( BACKGROUND_ARG,
                                colorsNodesWithChildren() )
                ) ) ) );
    }

}
