package com.athaydes.osgiaas.api.ansi;

public enum AnsiColor {

    // neutral
    RESET( 0 ),

    // foreground
    BLACK( 30 ),
    RED( 31 ),
    GREEN( 32 ),
    YELLOW( 33 ),
    BLUE( 34 ),
    PURPLE( 35 ),
    CYAN( 36 ),
    WHITE( 37 ),

    // background
    _BLACK( 40 ),
    _RED( 41 ),
    _GREEN( 42 ),
    _YELLOW( 43 ),
    _BLUE( 44 ),
    _PURPLE( 45 ),
    _CYAN( 46 ),
    _WHITE( 47 );

    public static final AnsiColor DEFAULT_BG = _YELLOW;

    private final int code;

    AnsiColor( int code ) {
        this.code = code;
    }

    @Override
    public String toString() {
        return Ansi.stringFromCode( code );
    }

}