package com.athaydes.osgiaas.api.ansi;

public enum AnsiModifier {

    RESET( 0 ),
    HIGH_INTENSITY( 1 ),
    LOW_INTENSITY( 2 ),
    ITALIC( 3 ),
    UNDERLINE( 4 ),
    BLINK( 5 ),
    RAPID_BLINK( 6 ),
    REVERSE_VIDEO( 7 ),
    INVISIBLE_TEXT( 8 );

    private final int code;

    AnsiModifier( int code ) {
        this.code = code;
    }

    @Override
    public String toString() {
        return Ansi.stringFromCode( code );
    }

}
