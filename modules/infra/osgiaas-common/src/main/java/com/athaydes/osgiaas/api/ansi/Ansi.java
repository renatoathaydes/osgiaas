package com.athaydes.osgiaas.api.ansi;

import javax.annotation.Nullable;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Functions to help format text with ANSI.
 */
public class Ansi {

    // TODO also check byte patterns
    public static final Pattern ANSI_PATTERN = Pattern.compile( "(\\u001B)?\\[\\d*m" );

    static String simpleAnsiEscape( int code ) {
        return "\u001B[" + code + "m";
    }

    /**
     * @param b color byte
     * @return escape sequence for the foreground color represented by this byte (must in the range 0..255).
     */
    public static String foreColorEscape( int b ) {
        if ( -1 < b && b < 256 ) {
            return "\u001B[38;5;" + b + "m";
        }

        throw new IllegalArgumentException( "code not in the range 0..255" );
    }

    /**
     * @param b color byte
     * @return escape sequence for the background color represented by this byte (must in the range 0..255).
     */
    public static String backColorEscape( int b ) {
        if ( -1 < b && b < 256 ) {
            return "\u001B[48;5;" + b + "m";
        }

        throw new IllegalArgumentException( "code not in the range 0..255" );
    }

    /**
     * Apply the ANSI color to the the given text.
     * <p>
     * The RESET modifier is applied automatically at the end.
     *
     * @param text  to format
     * @param color color to apply.
     * @return formatted text
     */
    public static String applyColor( String text, AnsiColor color ) {
        return applyAnsi( text, color );
    }

    /**
     * Apply the ANSI color to the foreground of the given text.
     * <p>
     * The RESET modifier is applied automatically at the end.
     *
     * @param text      to format
     * @param colorByte color byte to apply to the foreground (from 0 to 255).
     * @return formatted text
     * @throws IllegalArgumentException if colorByte is not between 0 and 255
     */
    public static String applyForeColor( String text, int colorByte ) {
        return asString( join( foreColorEscape( colorByte ), text ) );
    }

    /**
     * Apply the ANSI color to the background of the given text.
     * <p>
     * The RESET modifier is applied automatically at the end.
     *
     * @param text      to format
     * @param colorByte color byte to apply to the background (from 0 to 255).
     * @return formatted text
     * @throws IllegalArgumentException if colorByte is not between 0 and 255
     */
    public static String applyBackColor( String text, int colorByte ) {
        return asString( join( backColorEscape( colorByte ), text ) );
    }

    /**
     * Apply the ANSI color + modifiers to the given text.
     * <p>
     * The RESET modifier is applied automatically at the end.
     *
     * @param text      to format
     * @param color     color to apply.
     * @param modifiers foreground modifiers
     * @return formatted text
     */
    public static String applyAnsi( String text, @Nullable AnsiColor color, AnsiModifier... modifiers ) {
        String[] modifierEscapers = Stream.of( modifiers ).map( AnsiModifier::toString ).toArray( String[]::new );
        return asString( join( color == null ? null : color.toString(), join( text, modifierEscapers ) ) );
    }

    /**
     * Apply the ANSI escapeSequences to the given text.
     * <p>
     * This method performs no validation of the escape sequences. Prefer the other overloads of this method,
     * which are type-safe.
     *
     * @param text                 to format
     * @param firstEscapeSequence  first escape sequence to apply
     * @param secondEscapeSequence second escape sequence to apply
     * @param escapeSequences      more escape sequences to apply
     * @return formatted text
     */
    public static String applyAnsi( String text,
                                    @Nullable String firstEscapeSequence,
                                    @Nullable String secondEscapeSequence,
                                    @Nullable String... escapeSequences ) {
        if ( escapeSequences == null ) {
            escapeSequences = new String[ 0 ];
        }

        // parts = first, second, [escapeSequences], text
        String[] parts = new String[ escapeSequences.length + 3 ];
        parts[ 0 ] = firstEscapeSequence;
        parts[ 1 ] = secondEscapeSequence;
        System.arraycopy( escapeSequences, 0, parts, 2, escapeSequences.length );
        parts[ parts.length - 1 ] = text;

        return asString( parts );
    }

    /**
     * Apply the ANSI escapeSequences to the given text.
     * <p>
     * This method performs no validation of the escape sequences. Prefer the other overloads of this method,
     * which are type-safe.
     *
     * @param text           to format
     * @param escapeSequence escape sequence to apply
     * @return formatted text
     */
    public static String applyAnsi( String text, @Nullable String escapeSequence ) {
        return applyAnsi( text, escapeSequence, null );
    }

    private static String asString( String... parts ) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if ( part != null ) {
                builder.append( part );
            }
        }
        builder.append( AnsiColor.RESET );
        return builder.toString();
    }

    private static String[] join( @Nullable String object, @Nullable String... others ) {
        if ( others == null || others.length == 0 ) {
            return new String[]{ object };
        }

        String[] result = new String[ others.length + 1 ];
        result[ 0 ] = object;
        System.arraycopy( others, 0, result, 1, others.length );
        return result;
    }

}
