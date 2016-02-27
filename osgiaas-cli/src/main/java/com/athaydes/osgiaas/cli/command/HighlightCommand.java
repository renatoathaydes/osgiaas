package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.ansi.Ansi;
import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.ansi.AnsiModifier;
import com.athaydes.osgiaas.cli.util.CommandHelper;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Highlight command.
 * <p>
 * Similar to grep, but instead of filtering input, it highlights input using different colors.
 */
public class HighlightCommand extends UsesCliProperties implements Command {

    private static final Pattern bfPattern = Pattern.compile(
            "\\s*highlight\\s+(-B\\s+([A-z]+)\\s+)?(-F\\s+([A-z\\+]+)\\s+)?(.+)\\s+(.+)",
            Pattern.DOTALL );

    private static final Pattern fbPattern = Pattern.compile(
            "\\s*highlight\\s+(-F\\s+([A-z\\+]+)\\s+)?(-B\\s+([A-z]+)\\s+)?(.+)\\s+(.+)",
            Pattern.DOTALL );

    @Override
    public String getName() {
        return "highlight";
    }

    @Override
    public String getUsage() {
        return "highlight [-F <color>] [-B <color>] <regex> <text-to-highlight>";
    }

    @Override
    public String getShortDescription() {
        return "Shows the lines of the given input that match a regular expression highlighted.\n" +
                "Because it is not possible to enter multiple lines manually, this command is often used to highlight " +
                "output from other commands via the '|' (pipe) operator.\n" +
                "The highlight command accepts the following flags:\n" +
                "  \n" +
                "  * -B <color>: highlighted text background color.\n" +
                "  * -F <color+modifier>: highlighted text foreground color and modifier(s).\n" +
                " \n" +
                "Example: ps | highlight -B red -F yellow+high_intensity";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {

        try {
            @Nullable HighlightCall highlightCall = highlightCall( line );
            int limit = getLimit( highlightCall );
            String[] parts = CommandHelper.breakupArguments( line, limit );
            highlightMatchingLines( out, err, highlightCall, limit, parts );
        } catch ( RuntimeException e ) {
            CommandHelper.printError( err, getUsage(), "Invalid arguments: " + e.getMessage() );
        }
    }

    private void highlightMatchingLines( PrintStream out, PrintStream err,
                                         @Nullable HighlightCall highlightCall,
                                         int limit, String[] parts ) {
        if ( limit > 0 && parts.length == limit ) {
            String regex = parts[ limit - 2 ];
            String text = parts[ limit - 1 ];
            try {
                highlight( regex, text, highlightCall, out );
            } catch ( PatternSyntaxException e ) {
                err.println( "Pattern syntax error in [" + regex + "]: " + e.getMessage() );
            }
        } else {
            CommandHelper.printError( err, getUsage(),
                    "Wrong number of arguments provided." );
        }
    }

    private static int getLimit( @Nullable HighlightCall highlightCall ) {
        if ( highlightCall == null ) {
            return -1; // just cause printError to be called
        } else switch ( highlightCall.getArgumentsGiven() ) {
            case 0:
                return 3;
            case 1:
                return 5;
            case 2:
            default:
                return 7;
        }
    }

    void highlight( String regex, String text,
                    @Nullable HighlightCall highlightCall,
                    PrintStream out ) {
        regex = ".*" + regex + ".*";
        String[] textLines = text.split( "\n" );

        //noinspection ForLoopReplaceableByForEach
        for (String txtLine : textLines) {
            boolean match = highlightCall != null && txtLine.matches( regex );
            if ( match ) {
                out.print( Ansi.applyAnsi( txtLine,
                        highlightCall.getColors(), highlightCall.getModifiers() ) );
                withCliProperties(
                        cliProperties -> out.println( cliProperties.getTextColor() ),
                        () -> out.println( AnsiColor.RESET ) );
            } else {
                out.println( txtLine );
            }
        }
    }

    @Nullable
    static HighlightCall highlightCall( String line ) {
        Matcher fbMatch = fbPattern.matcher( line );
        Matcher bfMatch = bfPattern.matcher( line );

        if ( fbMatch.matches() && bfMatch.matches() ) {
            String text = fbMatch.group( 6 );
            if ( text == null || text.isEmpty() ) {
                return null;
            }

            @Nullable String foreground;
            @Nullable String background;

            int abGroupCount = groupCount( fbMatch );
            int baGroupCount = groupCount( bfMatch );

            if ( abGroupCount > baGroupCount ) {
                foreground = fbMatch.group( 2 );
                background = fbMatch.group( 4 );
            } else {
                background = bfMatch.group( 2 );
                foreground = bfMatch.group( 4 );
            }

            return new HighlightCall( background, foreground );
        } else {
            return null;
        }
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

    static class HighlightCall {

        private final AnsiColor[] colors;
        private final AnsiModifier[] modifiers;
        private final int argumentsGiven;

        public HighlightCall( @Nullable String back, @Nullable String fore ) {
            this.argumentsGiven = back != null && fore != null ? 2 :
                    back == null && fore == null ? 0 : 1;

            AnsiColor background = back == null ?
                    AnsiColor._YELLOW :
                    parse( "_" + back, AnsiColor::valueOf );

            if ( fore != null ) {
                String[] foreParts = fore.split( Pattern.quote( "+" ) );
                AnsiColor foreColor = parse( foreParts[ 0 ], AnsiColor::valueOf );
                this.colors = new AnsiColor[]{ background, foreColor };
                this.modifiers = getAnsiModifiers( foreParts );
            } else {
                this.colors = new AnsiColor[]{ background };
                this.modifiers = new AnsiModifier[ 0 ];
            }
        }

        private AnsiModifier[] getAnsiModifiers( String[] foreParts ) {
            List<AnsiModifier> modifiers = new ArrayList<>( 2 );
            for (int i = 1; i < foreParts.length; i++) {
                modifiers.add( parse( foreParts[ i ], AnsiModifier::valueOf ) );
            }
            return modifiers.toArray( new AnsiModifier[ modifiers.size() ] );
        }

        private static <T> T parse( String text, Function<String, T> convert ) {
            try {
                return convert.apply( text.toUpperCase() );
            } catch ( IllegalArgumentException e ) {
                throw new RuntimeException( "Invalid argument: '" + text + "'" );
            }
        }

        public int getArgumentsGiven() {
            return argumentsGiven;
        }

        public AnsiColor[] getColors() {
            return colors;
        }

        public AnsiModifier[] getModifiers() {
            return modifiers;
        }
    }

}
