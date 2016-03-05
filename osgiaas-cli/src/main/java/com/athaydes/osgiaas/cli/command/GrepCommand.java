package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.StreamingCommand;
import com.athaydes.osgiaas.api.stream.LineOutputStream;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GrepCommand implements StreamingCommand {

    private static final Pattern baPattern = Pattern.compile(
            "\\s*grep\\s+(-B\\s+(\\d+)\\s+)?(-A\\s+(\\d+)\\s+)?([^\\s]+)(\\s+(.*))?",
            Pattern.DOTALL );

    private static final Pattern abPattern = Pattern.compile(
            "\\s*grep\\s+(-A\\s+(\\d+)\\s+)?(-B\\s+(\\d+)\\s+)?([^\\s]+)(\\s+(.*))?",
            Pattern.DOTALL );

    @Override
    public String getName() {
        return "grep";
    }

    @Override
    public String getUsage() {
        return "grep [-B <num>] [-A <num>] <regex> <text-to-search>";
    }

    @Override
    public String getShortDescription() {
        return "Shows only input text lines matching a regular expression.\n" +
                "Because it is not possible to enter multiple lines manually, this command is often used to filter " +
                "output from other commands via the '|' (pipe) operator.\n" +
                "The grep command accepts the following flags:\n" +
                "  \n" +
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
        @Nullable GrepCall grepCall = grepCall( line );


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

    static Consumer<String> grep( String regex,
                                  @Nullable GrepCall grepCall,
                                  Consumer<String> lineConsumer ) {
        Pattern regexPattern = Pattern.compile( ".*" + regex + ".*" );

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

    @Nullable
    static GrepCall grepCall( String line ) {
        Matcher abMatch = abPattern.matcher( line );
        Matcher baMatch = baPattern.matcher( line );

        if ( abMatch.matches() && baMatch.matches() ) {
            String regex;
            @Nullable String text;
            @Nullable String after;
            @Nullable String before;

            Matcher matcher = selectBestMatcher( abMatch, baMatch );

            after = matcher.group( matcher == abMatch ? 2 : 4 );
            before = matcher.group( matcher == baMatch ? 2 : 4 );
            regex = matcher.group( 5 );
            text = matcher.group( 7 );

            int beforeLines = parseInt( before );
            int afterLines = parseInt( after );

            return new GrepCall(
                    beforeLines, before != null,
                    afterLines, after != null,
                    regex, text == null ? "" : text );
        } else {
            return null;
        }
    }

    private static Matcher selectBestMatcher( Matcher m1, Matcher m2 ) {
        int count1 = optionalValuesCount( m1 );
        int count2 = optionalValuesCount( m2 );
        if ( count1 > count2 ) {
            return m1;
        } else {
            return m2;
        }
    }

    private static int optionalValuesCount( Matcher matcher ) {
        int count = 0;
        if ( matcher.group( 2 ) != null ) count++;
        if ( matcher.group( 4 ) != null ) count++;
        return count;
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

        public GrepCall( int beforeLines, boolean beforeGiven,
                         int afterLines, boolean afterGiven,
                         String regex, String text ) {
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
                    ", regex=" + regex +
                    ", textLength=" + text.length() +
                    '}';
        }
    }

}
