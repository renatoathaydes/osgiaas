package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.StreamingCommand;
import com.athaydes.osgiaas.cli.args.ArgsSpec;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GrepCommand implements StreamingCommand {

    public static final String BEFORE_ARG = "-B";
    public static final String BEFORE_LONG_ARG = "--before-context";
    public static final String AFTER_ARG = "-A";
    public static final String AFTER_LONG_ARG = "--after-context";
    public static final String CASE_INSENSITIVE_ARG = "-i";
    public static final String CASE_INSENSITIVE_LONG_ARG = "--ignore-case";

    private final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( BEFORE_ARG, BEFORE_LONG_ARG ).withArgs( "lines" )
            .withDescription( "number of lines to print before each match" ).end()
            .accepts( AFTER_ARG, AFTER_LONG_ARG ).withArgs( "lines" )
            .withDescription( "number of lines to print after each match" ).end()
            .accepts( CASE_INSENSITIVE_ARG, CASE_INSENSITIVE_LONG_ARG )
            .withDescription( "case insensitive regex" ).end()
            .build();

    @Override
    public String getName() {
        return "grep";
    }

    @Override
    public String getUsage() {
        return "grep " + argsSpec.getUsage() + " <regex> <text-to-search>";
    }

    @Override
    public String getShortDescription() {
        return "Shows only input text lines matching a regular expression.\n" +
                "This command is often used to filter " +
                "output from other commands via the '|' (pipe) operator (see example).\n\n" +
                "The grep command accepts the following options:\n\n" +
                argsSpec.getDocumentation( "  " ) + "\n\n" +
                "Example usage (find all lines printed by the 'ps' command containing 'cli'):\n\n" +
                "> ps | grep cli\n";
    }

    @Override
    public Consumer<String> pipe( String line, PrintStream out, PrintStream err ) {
        @Nullable Consumer<String> consumer = grepAndConsume( line, out, err );
        if ( consumer != null ) {
            return consumer;
        } else {
            return ( ignore ) -> {
                // nothing to do
            };
        }
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        grepAndConsume( line, out, err );
    }

    @Nullable
    Consumer<String> grepAndConsume( String line, PrintStream out, PrintStream err ) {
        @Nullable GrepCall grepCall = grepCall( line, err );

        if ( grepCall != null ) {
            String regex = grepCall.regex;
            String text = grepCall.text;

            try {
                Consumer<String> consumer = grep( regex, grepCall, out::println );
                for (String txtLine : text.split( "\n" )) {
                    consumer.accept( txtLine );
                }
                return consumer;
            } catch ( PatternSyntaxException e ) {
                err.println( "Pattern syntax error in [" + regex + "]: " + e.getMessage() );
            }
        } else {
            CommandHelper.printError( err, getUsage(),
                    "Wrong number of arguments provided." );
        }

        return null;
    }

    private static Consumer<String> grep( String regex,
                                          @Nullable GrepCall grepCall,
                                          Consumer<String> lineConsumer ) {
        Pattern regexPattern = shouldUseCaseInsensitiveRegex( grepCall ) ?
                Pattern.compile( ".*" + regex + ".*", Pattern.CASE_INSENSITIVE ) :
                Pattern.compile( ".*" + regex + ".*" );

        // large number that can be safely added to without overflow
        final AtomicInteger pastLastMatch = new AtomicInteger( 1 << 30 );
        int beforeLines = grepCall == null ? 0 : grepCall.beforeLines;
        int afterLines = grepCall == null ? 0 : grepCall.afterLines;

        LinkedList<String> textLines = new LinkedList<>();

        return txtLine -> {
            boolean match = regexPattern.matcher( txtLine ).matches();
            if ( match ) {
                pastLastMatch.set( 0 );
                while ( !textLines.isEmpty() ) {
                    lineConsumer.accept( textLines.removeFirst() );
                }
            }

            if ( pastLastMatch.getAndIncrement() <= afterLines ) {
                lineConsumer.accept( txtLine );
            } else if ( beforeLines > 0 ) {
                textLines.add( txtLine );
                if ( textLines.size() > beforeLines ) {
                    textLines.removeFirst();
                }
            }
        };
    }

    private static boolean shouldUseCaseInsensitiveRegex( @Nullable GrepCall grepCall ) {
        return grepCall != null && grepCall.caseInsensitive;
    }

    @Nullable
    GrepCall grepCall( String line, PrintStream err ) {
        CommandInvocation invocation;

        try {
            invocation = argsSpec.parse( line );
        } catch ( IllegalArgumentException e ) {
            CommandHelper.printError( err, getUsage(), e.getMessage() );
            return null;
        }

        boolean caseInsensitive = invocation.hasOption( CASE_INSENSITIVE_ARG );
        Optional<String> after = invocation.getOptionalFirstArgument( AFTER_ARG );
        Optional<String> before = invocation.getOptionalFirstArgument( BEFORE_ARG );

        List<String> rest = CommandHelper.breakupArguments( invocation.getUnprocessedInput(), 2 );

        if ( rest.isEmpty() ) {
            CommandHelper.printError( err, getUsage(), "Wrong number of arguments provided." );
            return null;
        }

        String regex = rest.get( 0 );
        @Nullable String text = rest.size() > 1 ? rest.get( 1 ) : null;

        try {
            return new GrepCall(
                    caseInsensitive,
                    Integer.parseInt( before.orElse( "0" ) ), before.isPresent(),
                    Integer.parseInt( after.orElse( "0" ) ), after.isPresent(),
                    regex, text == null ? "" : text );
        } catch ( NumberFormatException nfe ) {
            CommandHelper.printError( err, getUsage(), "Expected integer argument for " +
                    BEFORE_ARG + " or " + AFTER_ARG );
            return null;
        }
    }

    static class GrepCall {
        final int beforeLines;
        final boolean beforeGiven;
        final int afterLines;
        final boolean afterGiven;
        final String regex;
        final String text;
        final boolean caseInsensitive;

        GrepCall( boolean caseInsensitive,
                  int beforeLines, boolean beforeGiven,
                  int afterLines, boolean afterGiven,
                  String regex, String text ) {
            this.caseInsensitive = caseInsensitive;
            this.beforeLines = beforeLines;
            this.beforeGiven = beforeGiven;
            this.afterLines = afterLines;
            this.afterGiven = afterGiven;
            this.regex = regex;
            this.text = text;
        }

        @Override
        public String toString() {
            return "GrepCall{" +
                    "beforeLines=" + beforeLines +
                    ", beforeGiven=" + beforeGiven +
                    ", afterLines=" + afterLines +
                    ", afterGiven=" + afterGiven +
                    ", regex='" + regex + '\'' +
                    ", text='" + text + '\'' +
                    ", caseInsensitive=" + caseInsensitive +
                    '}';
        }
    }

}
