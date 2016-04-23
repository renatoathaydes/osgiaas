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

import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.nameMatcher;

public class GrepCommandCompleter extends BaseCompleter {

    public GrepCommandCompleter() {
        super( nameMatcher( "grep", completers() ) );
    }

    private static Supplier<Stream<CompletionMatcher>> completers() {
        CompletionMatcher intMatcher = new IntCompletionMatcher(
                nameMatcher( GrepCommand.CASE_INSENSITIVE_ARG ) );

        CompletionMatcher beforeThenAfter = nameMatcher(
                GrepCommand.AFTER_ARG, new IntCompletionMatcher(
                        nameMatcher( GrepCommand.BEFORE_ARG, intMatcher ) )
        );
        CompletionMatcher afterThenBefore = nameMatcher( GrepCommand.AFTER_ARG, new IntCompletionMatcher(
                nameMatcher( GrepCommand.BEFORE_ARG, intMatcher )
        ) );

        return () -> Stream.of(
                nameMatcher( GrepCommand.CASE_INSENSITIVE_ARG, afterThenBefore ),
                nameMatcher( GrepCommand.CASE_INSENSITIVE_ARG, beforeThenAfter ),
                afterThenBefore,
                beforeThenAfter );
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
