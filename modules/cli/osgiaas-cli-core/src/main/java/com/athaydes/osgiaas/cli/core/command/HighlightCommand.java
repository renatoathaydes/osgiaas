package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.api.ansi.Ansi;
import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.ansi.AnsiModifier;
import com.athaydes.osgiaas.api.stream.LineOutputStream;
import com.athaydes.osgiaas.api.stream.NoOpPrintStream;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.StreamingCommand;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import com.athaydes.osgiaas.cli.core.util.UsesCliProperties;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Highlight command.
 * <p>
 * Similar to grep, but instead of filtering input, it highlights input using different colors.
 */
public class HighlightCommand extends UsesCliProperties implements StreamingCommand {

    public static final Map<String, String> ansiModifierNameByShortOption;

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
        map.accept( "li", AnsiModifier.LOW_INTENSITY.name() );
        map.accept( "it", AnsiModifier.INVISIBLE_TEXT.name() );
        map.accept( "rb", AnsiModifier.RAPID_BLINK.name() );
        map.accept( "u", AnsiModifier.UNDERLINE.name() );
        map.accept( "rv", AnsiModifier.REVERSE_VIDEO.name() );

        ansiModifierNameByShortOption = Collections.unmodifiableMap( argumentByShortArgMap );
    }

    public static final String FOREGROUND_ARG = "-f";
    public static final String BACKGROUND_ARG = "-b";
    public static final String CASE_INSENSITIVE_ARG = "-i";

    private final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( FOREGROUND_ARG ).withArgCount( 1 ).end()
            .accepts( BACKGROUND_ARG ).withArgCount( 1 ).end()
            .accepts( CASE_INSENSITIVE_ARG ).end()
            .build();

    @Override
    public String getName() {
        return "highlight";
    }

    @Override
    public String getUsage() {
        return "highlight [-i] [-f <color>] [-b <color>] <regex> <text-to-highlight>";
    }

    @Override
    public String getShortDescription() {
        return "Shows the lines of the given input that match a regular expression highlighted.\n" +
                "Because it is not possible to enter multiple lines manually, this command is often used to highlight " +
                "output from other commands via the '|' (pipe) operator.\n" +
                "The highlight command accepts the following flags:\n" +
                "  \n" +
                "  * -i: case insensitive regex.\n" +
                "  * -b <color>: highlighted text background color.\n" +
                "  * -f <color+modifier>: highlighted text foreground color and modifier(s).\n" +
                " \n" +
                "Example: ps | highlight -b red -f yellow+high_intensity";
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
    private CommandInvocation parseInvocation( String line, PrintStream err ) {
        try {
            return argsSpec.parse( line );
        } catch ( IllegalArgumentException e ) {
            CommandHelper.printError( err, getUsage(), e.getMessage() );
            return null;
        }
    }

    @Nullable
    HighlightCall highlightCall( String line, PrintStream err ) {
        CommandInvocation invocation = parseInvocation( line, err );
        if ( invocation == null ) {
            return null;
        } else {
            boolean caseInsensitive = invocation.hasArg( CASE_INSENSITIVE_ARG );
            @Nullable String foreground = invocation.getArgValue( FOREGROUND_ARG );
            @Nullable String background = invocation.getArgValue( BACKGROUND_ARG );
            List<String> rest = CommandHelper.breakupArguments(
                    invocation.getUnprocessedInput(), 2 );

            if ( rest.isEmpty() ) {
                CommandHelper.printError( err, getUsage(), "Wrong number of arguments provided." );
            } else {
                String regex = rest.get( 0 );
                String input = ( rest.size() == 2 ) ? rest.get( 1 ) : "";
                @Nullable Pattern matchPattern = getPattern( regex, caseInsensitive, err );

                if ( matchPattern != null ) {
                    return new HighlightCall( background, foreground, matchPattern, input, getTextColor() );
                }
            }
        }

        return null;
    }

    @Nullable
    private Pattern getPattern( String regex, boolean caseInsensitive, PrintStream err ) {
        try {
            int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
            return Pattern.compile( ".*" + regex + ".*", flags );
        } catch ( PatternSyntaxException e ) {
            err.println( "Pattern syntax error in [" + regex + "]: " + e.getMessage() );
            return null;
        }
    }

    private static class HighlightCall {

        private final AnsiColor[] colors;
        private final AnsiModifier[] modifiers;
        private final Pattern pattern;
        private final String text;
        private final String originalColor;

        HighlightCall( @Nullable String back, @Nullable String fore,
                       Pattern pattern, String text, String originalColor ) {
            this.pattern = pattern;
            this.text = text;
            this.originalColor = originalColor;

            AnsiColor background = ( back == null ) ?
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
                modifiers.add( parse( foreParts[ i ], AnsiModifier::valueOf, ansiModifierNameByShortOption ) );
            }
            return modifiers.toArray( new AnsiModifier[ modifiers.size() ] );
        }

        private static <T> T parse( String text, Function<String, T> convert,
                                    @Nullable Map<String, String> argMapper ) {
            @Nullable String mappedText = argMapper == null ? null : argMapper.get( text );
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

        AnsiColor[] getColors() {
            return colors;
        }

        AnsiModifier[] getModifiers() {
            return modifiers;
        }

        Pattern getPattern() {
            return pattern;
        }

        String getText() {
            return text;
        }

        String getOriginalColor() {
            return originalColor;
        }
    }

}
