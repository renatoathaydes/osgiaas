package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.command.HighlightCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.command.HighlightCommand.BACKGROUND_ARG;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.FOREGROUND_ARG;

public class HighlightCommandCompleter extends BaseCompleter {

    private static final List<CompletionMatcher> ansiModifierNodes =
            HighlightCommand.argumentByShortArg.entrySet().stream()
                    .flatMap( entry -> Stream.of(
                            CompletionMatcher.nameMatcher( entry.getKey() ), // short-form
                            CompletionMatcher.nameMatcher( entry.getValue().toLowerCase() ) ) ) // long-form
                    .collect( Collectors.toList() );

    private static final List<CompletionMatcher> colorNodes = ColorCommandCompleter
            .colorsNodesWithChildren( Collections.emptyList() );

    private static CompletionMatcher argMatchers( String firstArg,
                                                  String secondArg,
                                                  List<List<CompletionMatcher>> argOptions ) {
        CompletionMatcher secondArgOptions = CompletionMatcher.nameMatcher( secondArg,
                Collections.singletonList( CompletionMatcher.multiPartMatcher( "+", argOptions ) ) );

        List<CompletionMatcher> firstArgOptions = Collections.singletonList(
                CompletionMatcher.multiPartMatcher( "+", argOptions,
                        Collections.singletonList( secondArgOptions ) ) );

        return CompletionMatcher.nameMatcher( firstArg, firstArgOptions );
    }

    private static List<CompletionMatcher> nodesForHighlightCommand() {
        List<List<CompletionMatcher>> argOptions = Arrays.asList( colorNodes, ansiModifierNodes );

        return Arrays.asList(
                argMatchers( BACKGROUND_ARG, FOREGROUND_ARG, argOptions ),
                argMatchers( FOREGROUND_ARG, BACKGROUND_ARG, argOptions ) );
    }

    public HighlightCommandCompleter() {
        super( CompletionMatcher.nameMatcher( "highlight", nodesForHighlightCommand() ) );
    }

}
