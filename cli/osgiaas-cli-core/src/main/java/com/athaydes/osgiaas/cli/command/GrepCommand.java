package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandInvocation;
import com.athaydes.osgiaas.api.cli.StreamingCommand;
import com.athaydes.osgiaas.api.cli.args.ArgsSpec;
import com.athaydes.osgiaas.api.stream.LineOutputStream;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GrepCommand implements StreamingCommand {

    public static final String BEFORE_ARG = "-B";
    public static final String AFTER_ARG = "-A";
    public static final String CASE_INSENSITIVE_ARG = "-i";

    private final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( BEFORE_ARG, false, true )
            .accepts( AFTER_ARG, false, true )
            .accepts( CASE_INSENSITIVE_ARG )
            .build();

    @Override
    public String getName() {
        return "grep";
    }

    @Override
    public String getUsage() {
        return "grep [-B <num>] [-A <num>] [-i] <regex> <text-to-search>";
    }

    @Override
    public String getShortDescription() {
        return "Shows only input text lines matching a regular expression.\n" +
                "Because it is not possible to enter multiple lines manually, this command is often used to filter " +
                "output from other commands via the '|' (pipe) operator.\n" +
                "The grep command accepts the following flags:\n" +
                "  \n" +
                "  * -i: case insensitive regex.\n" +
                "  * -B <num>: num is the number of lines to print before each match.\n" +
                "  * -A <num>: num is the number of lines to print after each match.";
    }

    @Override
    public LineOutputStream pipe( String line, PrintStream out, PrintStream err ) {
        @Nullable Consumer<String> consumer = grepAndConsume( line, out, err );
        if ( consumer != null ) {
            return new LineOutputStream( consumer, out );
        } else {
            return new LineOutputStream( ( ignore ) -> {
            }, out );
        }
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        grepAndConsume( line, out, err );
    }

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

        boolean caseInsensitive = invocation.hasArg( CASE_INSENSITIVE_ARG );
        @Nullable String after = invocation.getArgValue( AFTER_ARG );
        @Nullable String before = invocation.getArgValue( BEFORE_ARG );

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
                    parseInt( before ), before != null,
                    parseInt( after ), after != null,
                    regex, text == null ? "" : text );
        } catch ( NumberFormatException nfe ) {
            CommandHelper.printError( err, getUsage(), "Expected integer argument for " +
                    BEFORE_ARG + " or " + AFTER_ARG );
            return null;
        }
    }

    private static int parseInt( @Nullable String arg ) {
        return ( arg == null ) ?
                0 :
                Integer.parseInt( arg );
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
