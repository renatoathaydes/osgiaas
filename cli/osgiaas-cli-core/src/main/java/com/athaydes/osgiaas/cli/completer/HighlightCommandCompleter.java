package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.command.HighlightCommand;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.alternativeMatchers;
import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.multiPartMatcher;
import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.nameMatcher;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.BACKGROUND_ARG;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.CASE_INSENSITIVE_ARG;
import static com.athaydes.osgiaas.cli.command.HighlightCommand.FOREGROUND_ARG;
import static com.athaydes.osgiaas.cli.completer.ColorCommandCompleter.colorsNodesWithChildren;

public class HighlightCommandCompleter extends BaseCompleter {

    static CompletionMatcher ansiModifierNodesWithChildren(
            Supplier<Stream<CompletionMatcher>> children ) {
        Set<String> shortFormAnsiModifiers = HighlightCommand.ansiModifierNameByShortOption.keySet();

        Set<String> longFormAnsiModifiers = HighlightCommand.ansiModifierNameByShortOption.values().stream()
                .map( String::toLowerCase )
                .collect( Collectors.toSet() );

        return multiPartMatcher( "+", Arrays.asList(
                alternativeMatchers( colorsNodesWithChildren() ),
                alternativeMatchers(
                        asMatchers( shortFormAnsiModifiers ),
                        asMatchers( longFormAnsiModifiers )
                ) ), children );
    }

    private static CompletionMatcher asMatchers( Set<String> shortFormAnsiModifiers,
                                                 CompletionMatcher... children ) {
        return alternativeMatchers( () -> shortFormAnsiModifiers.stream()
                .sorted()
                .map( modifier -> nameMatcher( modifier, children ) ) );
    }

    private static CompletionMatcher backThenFore( Supplier<Stream<CompletionMatcher>> children ) {
        return nameMatcher( BACKGROUND_ARG, colorsNodesWithChildren(
                nameMatcher( FOREGROUND_ARG,
                        ansiModifierNodesWithChildren( children ) )
        ) );
    }

    private static CompletionMatcher foreThenBack( Supplier<Stream<CompletionMatcher>> children ) {
        return nameMatcher( FOREGROUND_ARG, colorsNodesWithChildren(
                nameMatcher( BACKGROUND_ARG,
                        ansiModifierNodesWithChildren( children ) )
        ) );
    }

    private static Supplier<Stream<CompletionMatcher>> caseInsensitive( Supplier<Stream<CompletionMatcher>> children ) {
        return () -> Stream.of( nameMatcher( CASE_INSENSITIVE_ARG, children ) );
    }

    private static Supplier<Stream<CompletionMatcher>> completers() {
        Supplier<Stream<CompletionMatcher>> followingCaseInsensitiveOptions = () ->
                Stream.of( alternativeMatchers(
                        backThenFore( Stream::empty ),
                        foreThenBack( Stream::empty ) ) );

        return () -> Stream.concat( caseInsensitive( followingCaseInsensitiveOptions ).get(),
                Stream.of(
                        backThenFore( caseInsensitive( Stream::empty ) ),
                        foreThenBack( caseInsensitive( Stream::empty ) )
                ) );
    }

    public HighlightCommandCompleter() {
        super( nameMatcher( "highlight", completers() ) );
    }

}
