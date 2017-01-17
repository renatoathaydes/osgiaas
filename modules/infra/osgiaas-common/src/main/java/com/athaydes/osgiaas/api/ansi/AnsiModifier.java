package com.athaydes.osgiaas.api.ansi;

/**
 * Enumerator containing ANSI modifiers.
 * <p>
 * ANSI modifiers can be applied together with {@link AnsiColor}.
 *
 * @see Ansi#applyAnsi(String, AnsiColor, AnsiModifier...)
 */
public enum AnsiModifier {

    RESET( 0 ),
    BOLD( 1 ),
    DIM( 2 ),
    UNDERLINE( 4 ),
    BLINK( 5 ),
    RAPID_BLINK( 6 ),
    REVERSE( 7 ),
    HIDDEN( 8 );

    private final int code;

    AnsiModifier( int code ) {
        this.code = code;
    }

    @Override
    public String toString() {
        return Ansi.simpleAnsiEscape( code );
    }

}
