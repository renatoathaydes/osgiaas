package com.athaydes.osgiaas.api.ansi;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enumerator containing simple ANSI Colors.
 *
 * @see AnsiModifier
 */
public enum AnsiColor {

    // neutral
    RESET( 0 ),
    FOREGROUND_DEFAULT( 39 ),

    // foreground
    BLACK( 30 ),
    RED( 31 ),
    GREEN( 32 ),
    YELLOW( 33 ),
    BLUE( 34 ),
    PURPLE( 35 ),
    CYAN( 36 ),
    LIGHT_GRAY( 37 ),
    DARK_GRAY( 90 ),
    LIGHT_RED( 91 ),
    LIGHT_GREEN( 92 ),
    LIGHT_YELLOW( 93 ),
    LIGHT_BLUE( 94 ),
    LIGHT_PURPLE( 95 ),
    LIGHT_CYAN( 96 ),
    WHITE( 97 ),

    // background
    BKG_DEFAULT( 49 ),
    BKG_BLACK( 40 ),
    BKG_RED( 41 ),
    BKG_GREEN( 42 ),
    BKG_YELLOW( 43 ),
    BKG_BLUE( 44 ),
    BKG_PURPLE( 45 ),
    BKG_CYAN( 46 ),
    BKG_LIGHT_GRAY( 47 ),
    BKG_DARK_GRAY( 100 ),
    BKG_LIGHT_RED( 101 ),
    BKG_LIGHT_GREEN( 102 ),
    BKG_LIGHT_YELLOW( 103 ),
    BKG_LIGHT_BLUE( 104 ),
    BKG_LIGHT_PURPLE( 105 ),
    BKG_LIGHT_CYAN( 106 ),

    BKG_WHITE( 107 );

    public static final Pattern NUMBER_PATTERN = Pattern.compile( "\\d{1,3}" );

    public static final AnsiColor DEFAULT_BG = BKG_YELLOW;
    public static final String BKG_PREFIX = "BKG_";

    private final int code;

    AnsiColor( int code ) {
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
     * @param text to verify
     * @return true if the provided color is one of the named colors or a valid color byte, false otherwise.
     * If this method returns true, it is safe to call one of the parse methods.
     * @see #backColorEscapeCode(String)
     * @see #foreColorEscapeCode(String)
     */
    public static boolean isColor( String text ) {
        String option1 = text.toUpperCase();
        String option2 = BKG_PREFIX + option1;

        return Stream.of( values() ).anyMatch( it ->
                it.name().equals( option1 ) || it.name().equals( option2 ) ) ||
                isValidCode( text );
    }

    /**
     * Check if the given text can be used to generate a ANSI color escape sequence using one of the methods:
     * {@link #backColorEscapeCode(String)} and {@link #foreColorEscapeCode(String)}.
     *
     * @param text to check
     * @return true if the given text can be used to obtain a color escape code.
     */
    static boolean isValidCode( String text ) {
        if ( NUMBER_PATTERN.matcher( text ).matches() ) {
            int b = Integer.parseInt( text );
            return -1 < b && b < 256;
        } else {
            return false;
        }
    }

    /**
     * Get the background color escape code for the provided named color or color byte.
     *
     * @param color case-insensitive color name
     * @return escape sequence (can be prepended to any text to modify its colors)
     */
    public static String backColorEscapeCode( String color ) {
        if ( isValidCode( color ) ) { // try color code
            return Ansi.backColorEscape( Integer.parseInt( color ) );
        } else { // try named colors
            color = color.toUpperCase();
            if ( !color.startsWith( BKG_PREFIX ) ) {
                color = BKG_PREFIX + color;
            }
            return valueOf( color ).toString();
        }
    }

    /**
     * Get the foreground color escape code for the provided named color or color byte.
     *
     * @param color case-insensitive color name
     * @return escape sequence (can be prepended to any text to modify its colors)
     */
    public static String foreColorEscapeCode( String color ) {
        if ( isValidCode( color ) ) { // try color code
            return Ansi.foreColorEscape( Integer.parseInt( color ) );
        } else if ( colorNames().contains( color.toLowerCase() ) ) { // try named colors
            return valueOf( color.toUpperCase() ).toString();
        } else {
            throw new IllegalArgumentException( "not a valid color: " + color );
        }
    }

    /**
     * @return the simple name of the enumerated values for colors (not including the BKG_ prefix) in an
     * alphabetically sorted Set.
     */
    public static Set<String> colorNames() {
        return Arrays.stream( values() ).map( AnsiColor::name )
                .filter( it -> !it.startsWith( BKG_PREFIX ) )
                .map( String::toLowerCase )
                .sorted()
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

}