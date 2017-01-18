package com.athaydes.osgiaas.api.ansi;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * @return the escape code for the modifier. This value can be prepended to any text to modify it.
     */
    @Override
    public String toString() {
        return Ansi.simpleAnsiEscape( code );
    }

    /**
     * @return the simple name of the enumerated values for ANSI modifiers in an
     * alphabetically sorted Set.
     */
    public static Set<String> modifierNames() {
        return Arrays.stream( values() ).map( AnsiModifier::name )
                .map( String::toLowerCase )
                .sorted()
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }
}
