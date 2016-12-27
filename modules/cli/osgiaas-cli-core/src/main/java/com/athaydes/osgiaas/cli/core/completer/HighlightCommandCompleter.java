package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.core.command.HighlightCommand;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.alternativeMatchers;
import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.multiPartMatcher;
import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.nameMatcher;
import static com.athaydes.osgiaas.cli.core.command.HighlightCommand.BACKGROUND_ARG;
import static com.athaydes.osgiaas.cli.core.command.HighlightCommand.CASE_INSENSITIVE_ARG;
import static com.athaydes.osgiaas.cli.core.command.HighlightCommand.FOREGROUND_ARG;

public class HighlightCommandCompleter extends BaseCompleter {

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
        return nameMatcher( FOREGROUND_ARG, ansiModifierNodesWithChildren(
                () -> Stream.of( nameMatcher( BACKGROUND_ARG,
                        () -> Stream.of( colorsNodesWithChildren(
                                children.get().collect( Collectors.toList() ) ) ) ) )
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
