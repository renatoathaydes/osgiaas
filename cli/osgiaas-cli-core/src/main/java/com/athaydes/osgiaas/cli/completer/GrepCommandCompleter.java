package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.api.cli.completer.ParentCompletionMatcher;
import com.athaydes.osgiaas.cli.command.GrepCommand;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.alternativeMatchers;
import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.nameMatcher;

public class GrepCommandCompleter extends BaseCompleter {

    public GrepCommandCompleter() {
        super( nameMatcher( "grep", completers() ) );
    }

    private static CompletionMatcher intMatcher( CompletionMatcher... children ) {
        return new IntCompletionMatcher( children );
    }

    private static CompletionMatcher afterThenBefore( CompletionMatcher... children ) {
        return nameMatcher( GrepCommand.AFTER_ARG, intMatcher(
                nameMatcher( GrepCommand.BEFORE_ARG, intMatcher( children ) ) )
        );
    }

    private static CompletionMatcher beforeThenAfter( CompletionMatcher... children ) {
        return nameMatcher( GrepCommand.BEFORE_ARG, intMatcher(
                nameMatcher( GrepCommand.AFTER_ARG, intMatcher( children ) )
        ) );
    }

    private static CompletionMatcher caseInsensitive( CompletionMatcher... children ) {
        return nameMatcher( GrepCommand.CASE_INSENSITIVE_ARG, children );
    }

    private static Supplier<Stream<CompletionMatcher>> completers() {
        return () -> Stream.of(
                caseInsensitive( alternativeMatchers( afterThenBefore(), beforeThenAfter() ) ),
                afterThenBefore( caseInsensitive() ),
                beforeThenAfter( caseInsensitive() ) );
    }

    private static class IntCompletionMatcher extends ParentCompletionMatcher {

        private static final Pattern intPattern = Pattern.compile( "\\d{1,10}" );
        private static final Pattern partialIntPattern = Pattern.compile( "\\d{1,10}\\s.*" );

        public IntCompletionMatcher( CompletionMatcher... children ) {
            super( () -> Stream.of( children ) );
        }

        @Override
        public List<String> completionsFor( String argument ) {
            // cannot auto-complete numbers
            return Collections.emptyList();
        }

        @Override
        public boolean partiallyMatches( String command ) {
            return partialIntPattern.matcher( command ).matches();
        }

        @Override
        public boolean argumentFullyMatched( String argument ) {
            return intPattern.matcher( argument ).matches();
        }
    }
}
