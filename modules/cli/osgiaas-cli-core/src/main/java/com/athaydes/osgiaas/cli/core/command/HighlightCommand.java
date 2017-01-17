package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.api.ansi.Ansi;
import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.ansi.AnsiModifier;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.StreamingCommand;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import com.athaydes.osgiaas.cli.core.util.UsesCliProperties;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

        map.accept( "r", AnsiModifier.RESET.name() );
        map.accept( "bl", AnsiModifier.BLINK.name() );
        map.accept( "b", AnsiModifier.BOLD.name() );
        map.accept( "d", AnsiModifier.DIM.name() );
        map.accept( "h", AnsiModifier.HIDDEN.name() );
        map.accept( "rb", AnsiModifier.RAPID_BLINK.name() );
        map.accept( "u", AnsiModifier.UNDERLINE.name() );
        map.accept( "rv", AnsiModifier.REVERSE.name() );

        ansiModifierNameByShortOption = Collections.unmodifiableMap( argumentByShortArgMap );
    }

    public static final String FOREGROUND_ARG = "-f";
    public static final String FOREGROUND_LONG_ARG = "--foreground-color";
    public static final String BACKGROUND_ARG = "-b";
    public static final String BACKGROUND_LONG_ARG = "--background-color";
    public static final String CASE_INSENSITIVE_ARG = "-i";
    public static final String CASE_INSENSITIVE_LONG_ARG = "--ignore-case";

    public static final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( FOREGROUND_ARG, FOREGROUND_LONG_ARG ).withEnumeratedArg( "color[+modifier]",
                    HighlightCommand::foregroundColorAndModifiers )
            .withDescription( "highlighted text foreground color and modifier(s)" ).end()
            .accepts( BACKGROUND_ARG, BACKGROUND_LONG_ARG ).withEnumeratedArg( "color", AnsiColor::colorNames )
            .withDescription( "highlighted text background color" ).end()
            .accepts( CASE_INSENSITIVE_ARG, CASE_INSENSITIVE_LONG_ARG )
            .withDescription( "case insensitive regex" ).end()
            .build();

    private static List<String> foregroundColorAndModifiers() {
        List<String> result = new ArrayList<>( AnsiColor.values().length +
                ( AnsiColor.values().length * ansiModifierNameByShortOption.size() ) );
        result.addAll( AnsiColor.colorNames() );
        for (String ansiModifier : ansiModifierNameByShortOption.values()) {
            AnsiColor.colorNames().forEach( color -> result.add( color + "+" + ansiModifier.toLowerCase() ) );
        }
        return result;
    }

    @Override
    public String getName() {
        return "highlight";
    }

    @Override
    public String getUsage() {
        return "highlight " + argsSpec.getUsage() + " <regex> <text-to-highlight>";
    }

    @Override
    public String getShortDescription() {
        return "Highlight the input lines that match a given regular expression.\n" +
                "This command is often used to highlight " +
                "output from other commands via the '|' (pipe) operator.\n" +
                "The highlight command accepts the following options:\n\n" +
                argsSpec.getDocumentation( "  " ) + "\n\n" +
                "Example: ps | highlight -b red -f yellow+high_intensity";
    }

    @Override
    public Consumer<String> pipe( String command, PrintStream out, PrintStream err ) {
        @Nullable HighlightCall highlightCall = highlightCall( command, err );

        if ( highlightCall == null ) {
            return ( ignore ) -> {
                // do nothing
            };
        }

        return highlightMatchingLines( out, highlightCall );
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
                        highlightCall.getBackEscape(), highlightCall.getForeEscape(), highlightCall.getModifiers() ) );
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
            boolean caseInsensitive = invocation.hasOption( CASE_INSENSITIVE_ARG );

            @Nullable String foregroundPlusModifier = invocation.getOptionalFirstArgument( FOREGROUND_ARG )
                    .orElse( null );

            String background = invocation.getOptionalFirstArgument( BACKGROUND_ARG )
                    .orElse( AnsiColor.DEFAULT_BG.name() );

            String foreground;
            AnsiModifier[] modifiers;
            boolean error = false;

            if ( foregroundPlusModifier != null ) {
                String[] parts = foregroundPlusModifier.split( Pattern.quote( "+" ) );

                if ( parts.length < 1 || parts.length > 3 ) {
                    err.println( "Invalid foreground color: " + foregroundPlusModifier );
                    error = true;
                }

                modifiers = new AnsiModifier[ parts.length - 1 ];

                // parts.length is 1, 2 or 3 (first is color, 2nd and 3rd maybe modifiers)
                switch ( parts.length ) {
                    case 3:
                        modifiers[ 1 ] = parseAnsiModifier( parts[ 2 ] );
                    case 2:  // fall-through
                        modifiers[ 0 ] = parseAnsiModifier( parts[ 1 ] );
                    default: // fall-through
                    case 1:
                        foreground = parts[ 0 ];
                }
            } else {
                foreground = "";
                modifiers = new AnsiModifier[ 0 ];
            }

            if ( !AnsiColor.isColor( background ) ) {
                err.println( "Invalid background color: " + background );
                error = true;
            }
            if ( !AnsiColor.isColor( foreground ) ) {
                if ( !foreground.isEmpty() ) {
                    err.println( "Invalid foreground color: " + foreground );
                    error = true;
                }
            }

            if ( error ) {
                return null;
            }

            List<String> rest = CommandHelper.breakupArguments(
                    invocation.getUnprocessedInput(), 2 );

            if ( rest.isEmpty() ) {
                CommandHelper.printError( err, getUsage(), "Wrong number of arguments provided." );
            } else {
                String regex = rest.get( 0 );
                String input = ( rest.size() == 2 ) ? rest.get( 1 ) : "";
                @Nullable Pattern matchPattern = getPattern( regex, caseInsensitive, err );

                if ( matchPattern != null ) {
                    return new HighlightCall( AnsiColor.backColorEscapeCode( background ),
                            foreground.isEmpty() ? "" : AnsiColor.foreColorEscapeCode( foreground ),
                            modifiers, matchPattern, input, getTextColor() );
                }
            }
        }

        return null;
    }

    private static AnsiModifier parseAnsiModifier( String text ) {
        @Nullable String modifierName = ansiModifierNameByShortOption.get( text );

        String ansiModifier = text;
        if ( modifierName != null ) {
            ansiModifier = modifierName;
        }

        return AnsiModifier.valueOf( ansiModifier.toUpperCase() );
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

    static class HighlightCall {

        private final String backEscape;
        private final String foreEscape;
        private final AnsiModifier[] modifiers;
        private final Pattern pattern;
        private final String text;
        private final String originalColor;

        HighlightCall( String backgroundColor,
                       String foreColor,
                       AnsiModifier[] modifiers,
                       Pattern pattern,
                       String text,
                       String originalColor ) {
            this.backEscape = backgroundColor;
            this.foreEscape = foreColor;
            this.modifiers = modifiers;
            this.pattern = pattern;
            this.text = text;
            this.originalColor = originalColor;
        }

        String getBackEscape() {
            return backEscape;
        }

        String getForeEscape() {
            return foreEscape == null ? "" : foreEscape;
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
