package com.athaydes.osgiaas.api.ansi;

/**
 * Functions to help format text with ANSI.
 */
public class Ansi {

    static String stringFromCode( int code ) {
        return "\u001B[" + code + "m";
    }

    /**
     * Apply the ANSI color to the given text.
     *
     * @param text  to format
     * @param color color to apply.
     * @return formatted text
     */
    public static String applyColor( String text, AnsiColor color ) {
        return applyAnsi( text, new AnsiColor[]{ color } );
    }

    /**
     * Apply the ANSI colors and modifiers to the given text.
     *
     * @param text      to format
     * @param colors    colors to apply. Should be at most a foreground and a background color.
     * @param modifiers modifiers
     * @return formatted text
     */
    public static String applyAnsi( String text, AnsiColor[] colors, AnsiModifier... modifiers ) {
        return asString( join( modifiers, join( colors, text, AnsiModifier.RESET ) ) );
    }

    private static String asString( Object... objects ) {
        StringBuilder builder = new StringBuilder();
        for (Object obj : objects) {
            builder.append( obj );
        }
        return builder.toString();
    }

    private static Object[] join( Object[] objects, Object... others ) {
        Object[] result = new Object[ objects.length + others.length ];
        int i = 0;

        for (; i < objects.length; i++) {
            result[ i ] = objects[ i ];
        }
        for (; i < result.length; i++) {
            result[ i ] = others[ i - objects.length ];
        }

        return result;
    }

}
