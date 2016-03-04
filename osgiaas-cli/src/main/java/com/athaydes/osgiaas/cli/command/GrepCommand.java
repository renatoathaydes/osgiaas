package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.StreamingCommand;

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
            "\\s*grep\\s+(-B\\s+(\\d+)\\s+)?(-A\\s+(\\d+)\\s+)?(.+)\\s+(.+)",
            Pattern.DOTALL );

    private static final Pattern abPattern = Pattern.compile(
            "\\s*grep\\s+(-A\\s+(\\d+)\\s+)?(-B\\s+(\\d+)\\s+)?(.+)\\s+(.+)",
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
    public Consumer<String> pipe( String line, PrintStream out, PrintStream err ) {
        @Nullable Consumer<String> consumer = grepAndConsume( line, out, err );
        if ( consumer != null ) {
            return consumer;
        } else {
            return ( ignore ) -> {
            };
        }
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        grepAndConsume( line, out, err );
    }

    Consumer<String> grepAndConsume( String line, PrintStream out, PrintStream err ) {
        @Nullable GrepCall grepCall = grepCall( line );

        int limit = getLimit( grepCall );

        String[] parts = CommandHelper.breakupArguments( line, limit );

        if ( limit > 0 && parts.length == limit ) {
            String regex = parts[ limit - 2 ];
            String text = parts[ limit - 1 ];
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

    private static int getLimit( @Nullable GrepCall grepCall ) {
        int limit;
        if ( grepCall == null ) {
            limit = -1; // just cause printError to be called
        } else if ( grepCall.beforeGiven && grepCall.afterGiven ) {
            limit = 7;
        } else if ( grepCall.beforeGiven ^ grepCall.afterGiven ) { // either -B or -A given
            limit = 5;
        } else { // neither -B nor -A given
            limit = 3;
        }
        return limit;
    }

    @Nullable
    static GrepCall grepCall( String line ) {
        Matcher abMatch = abPattern.matcher( line );
        Matcher baMatch = baPattern.matcher( line );

        if ( abMatch.matches() && baMatch.matches() ) {
            String text = abMatch.group( 6 );
            if ( text == null || text.isEmpty() ) {
                return null;
            }

            @Nullable String after;
            @Nullable String before;

            int abGroupCount = groupCount( abMatch );
            int baGroupCount = groupCount( baMatch );

            if ( abGroupCount > baGroupCount ) {
                after = abMatch.group( 2 );
                before = abMatch.group( 4 );
            } else {
                before = baMatch.group( 2 );
                after = baMatch.group( 4 );
            }

            int beforeLines = parseInt( before );
            int afterLines = parseInt( after );

            return new GrepCall(
                    beforeLines, before != null,
                    afterLines, after != null );
        } else {
            return null;
        }
    }

    private static int parseInt( @Nullable String arg ) {
        return ( arg == null ) ?
                0 :
                Integer.parseInt( arg );
    }

    private static int groupCount( Matcher matcher ) {
        int result = 0;
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if ( matcher.group( i ) != null ) {
                result++;
            }
        }
        return result;
    }

    static class GrepCall {
        final int beforeLines;
        final boolean beforeGiven;
        final int afterLines;
        final boolean afterGiven;

        public GrepCall( int beforeLines, boolean beforeGiven,
                         int afterLines, boolean afterGiven ) {
            this.beforeLines = beforeLines;
            this.beforeGiven = beforeGiven;
            this.afterLines = afterLines;
            this.afterGiven = afterGiven;
        }
    }

}
