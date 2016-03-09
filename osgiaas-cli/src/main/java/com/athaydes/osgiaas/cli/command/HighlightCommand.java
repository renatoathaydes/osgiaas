package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.ansi.Ansi;
import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.ansi.AnsiModifier;
import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.StreamingCommand;
import com.athaydes.osgiaas.api.stream.LineOutputStream;
import com.athaydes.osgiaas.cli.util.NoOpPrintStream;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Highlight command.
 * <p>
 * Similar to grep, but instead of filtering input, it highlights input using different colors.
 */
public class HighlightCommand extends UsesCliProperties implements StreamingCommand {

    private static final Pattern bfPattern = Pattern.compile(
            "\\s*highlight\\s+(-B\\s+([A-z]+)\\s+)?(-F\\s+([A-z\\+]+)\\s+)?(.+)\\s+(.+)",
            Pattern.DOTALL );

    private static final Pattern fbPattern = Pattern.compile(
            "\\s*highlight\\s+(-F\\s+([A-z\\+]+)\\s+)?(-B\\s+([A-z]+)\\s+)?(.+)\\s+(.+)",
            Pattern.DOTALL );

    private static final Function<String, String> argumentByShortArg;

    static {
        Map<String, String> argumentByShortArgMap = new HashMap<>();

        BiConsumer<String, String> map = ( k, v ) -> {
            @Nullable Object existing = argumentByShortArgMap.put( k, v );
            if ( existing != null ) {
                throw new IllegalArgumentException( "Value for '" + k + "' already exists: " + existing );
            }
        };

        map.accept( "i", AnsiModifier.ITALIC.name() );
        map.accept( "r", AnsiModifier.RESET.name() );
        map.accept( "b", AnsiModifier.BLINK.name() );
        map.accept( "hi", AnsiModifier.HIGH_INTENSITY.name() );
        map.accept( "li", AnsiModifier.LOW_INTENSITy.name() );
        map.accept( "it", AnsiModifier.INVISIBLE_TEXT.name() );
        map.accept( "rb", AnsiModifier.RAPID_BLINK.name() );
        map.accept( "u", AnsiModifier.UNDERLINE.name() );
        map.accept( "rv", AnsiModifier.REVERSE_VIDEO.name() );

        argumentByShortArg = argumentByShortArgMap::get;
    }

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
    public OutputStream pipe( String command, PrintStream out, PrintStream err ) {
        @Nullable HighlightCall highlightCall = highlightCall( command, err );

        if ( highlightCall == null ) {
            return new NoOpPrintStream();
        }

        return new LineOutputStream( highlightMatchingLines( out, highlightCall ), out );
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        @Nullable HighlightCall highlightCall = highlightCall( line, err );

        if ( highlightCall != null ) {
            Consumer<String> consumer = highlightMatchingLines( out, highlightCall );
            String[] lines = highlightCall.getText().split( "\n" );
            for (String txtLine : lines) {
                consumer.accept( txtLine );
            }
        }
    }


    private Consumer<String> highlightMatchingLines( PrintStream out,
                                                     HighlightCall highlightCall ) {
        return line -> {
            boolean match = highlightCall.getPattern().matcher( line ).matches();
            if ( match ) {
                out.print( Ansi.applyAnsi(
                        Ansi.ANSI_PATTERN.matcher( line ).replaceAll( "" ),
                        highlightCall.getColors(), highlightCall.getModifiers() ) );
                out.println( highlightCall.getOriginalColor() );
            } else {
                out.println( line );
            }
        };
    }

    private String getTextColor() {
        AtomicReference<String> textColorRef = new AtomicReference<>();
        withCliProperties( cliProperties ->
                        textColorRef.set( AnsiColor.RESET.toString() + cliProperties.getTextColor() ),
                () -> textColorRef.set( AnsiColor.RESET.toString() ) );
        return textColorRef.get();
    }

    @Nullable
    HighlightCall highlightCall( String line, PrintStream err ) {
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

            int argumentsGiven = ( background != null && foreground != null ? 2 :
                    ( background == null && foreground == null ? 0 : 1 ) );

            int limit = getLimit( argumentsGiven );
            String[] parts = CommandHelper.breakupArguments( line, limit );

            if ( limit > 0 && parts.length == limit ) {
                String regex = parts[ limit - 2 ];
                String textColor = getTextColor();

                try {
                    Pattern matchPattern = Pattern.compile( ".*" + regex + ".*" );

                    return new HighlightCall( background, foreground, matchPattern, text, textColor );
                } catch ( PatternSyntaxException e ) {
                    err.println( "Pattern syntax error in [" + regex + "]: " + e.getMessage() );
                    return null;
                }
            }
        }

        CommandHelper.printError( err, getUsage(),
                "Wrong number of arguments provided." );
        return null;
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

    private static int getLimit( int argumentsGiven ) {
        switch ( argumentsGiven ) {
            case 0:
                return 3;
            case 1:
                return 5;
            case 2:
            default:
                return 7;
        }
    }

    static class HighlightCall {

        private final AnsiColor[] colors;
        private final AnsiModifier[] modifiers;
        private final int argumentsGiven;
        private final Pattern pattern;
        private final String text;
        private final String originalColor;

        public HighlightCall( @Nullable String back, @Nullable String fore,
                              Pattern pattern, String text, String originalColor ) {
            this.argumentsGiven = ( back != null && fore != null ? 2 :
                    ( back == null && fore == null ? 0 : 1 ) );
            this.pattern = pattern;
            this.text = text;
            this.originalColor = originalColor;

            AnsiColor background = back == null ?
                    AnsiColor.DEFAULT_BG :
                    parse( "_" + back, AnsiColor::valueOf, null );

            if ( fore != null ) {
                String[] foreParts = fore.split( Pattern.quote( "+" ) );
                AnsiColor foreColor = parse( foreParts[ 0 ], AnsiColor::valueOf, null );
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
                modifiers.add( parse( foreParts[ i ], AnsiModifier::valueOf, argumentByShortArg ) );
            }
            return modifiers.toArray( new AnsiModifier[ modifiers.size() ] );
        }

        private static <T> T parse( String text, Function<String, T> convert,
                                    @Nullable Function<String, String> argMapper ) {
            @Nullable String mappedText = argMapper == null ? null : argMapper.apply( text );
            try {
                if ( mappedText != null ) {
                    return convert.apply( mappedText );
                } else {
                    return convert.apply( text.toUpperCase() );
                }
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

        public Pattern getPattern() {
            return pattern;
        }

        public String getText() {
            return text;
        }

        public String getOriginalColor() {
            return originalColor;
        }
    }

}
