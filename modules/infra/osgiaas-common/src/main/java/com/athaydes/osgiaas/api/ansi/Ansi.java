package com.athaydes.osgiaas.api.ansi;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

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
        return asString( join( foreColorEscape( colorByte ), text, AnsiColor.RESET ) );
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
        return asString( join( backColorEscape( colorByte ), text, AnsiColor.RESET ) );
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
        return asString( join( join( color, text ), ( Object[] ) modifiers ) ) + AnsiColor.RESET;
    }

    /**
     * Apply the ANSI escapeSequences to the given text.
     * <p>
     * This method performs no validation of the escape sequences. Prefer the other overloads of this method,
     * which are type-safe.
     *
     * @param text            to format
     * @param escapeSequence  first escape sequence to apply
     * @param escapeSequences more escape sequences to apply
     * @return formatted text
     */
    public static String applyAnsi( String text,
                                    @Nullable Object escapeSequence,
                                    @Nullable Object... escapeSequences ) {
        if ( escapeSequences == null ) {
            escapeSequences = new Object[ 0 ];
        }

        // parts = escapeSequence, [escapeSequences], text, RESET
        Object[] parts = new Object[ escapeSequences.length + 3 ];
        parts[ 0 ] = escapeSequence;
        System.arraycopy( escapeSequences, 0, parts, 1, escapeSequences.length );
        parts[ parts.length - 2 ] = text;
        parts[ parts.length - 1 ] = AnsiColor.RESET;

        return asString( parts );
    }

    private static String asString( Object... objects ) {
        StringBuilder builder = new StringBuilder();
        for (Object obj : objects) {
            if ( obj != null ) {
                builder.append( obj );
            }
        }
        return builder.toString();
    }

    private static Object[] join( @Nullable Object object, @Nullable Object... others ) {
        if ( others == null || others.length == 0 ) {
            return new Object[]{ object };
        }

        Object[] result = new Object[ others.length + 1 ];
        result[ 0 ] = object;
        System.arraycopy( others, 0, result, 1, others.length );
        return result;
    }

}
