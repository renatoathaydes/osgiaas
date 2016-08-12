package com.athaydes.osgiaas.autocomplete.java.util;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class JavaLiterals {

    private static final Pattern CHAR_REGEX = Pattern.compile( "'[a-z|A-Z]'|'\\\\u[a-fA-F0-9]{4}'" );

    @Nullable
    public static Class<?> typeOfLiteral( String text ) {
        switch ( text ) {
            case "true":
            case "false":
                return boolean.class;
            case "void":
                return Void.class;
            default:
                return isChar( text ) ?
                        char.class :
                        isString( text ) ?
                                String.class :
                                isNumber( text ) ?
                                        numberTypeOf( text ) :
                                        null;
        }
    }

    private static Class<?> numberTypeOf( String text ) {
        char lastChar = text.charAt( text.length() - 1 );
        if ( text.contains( "." ) ) {
            if ( lastChar == 'f' || lastChar == 'F' ) {
                return float.class;
            } else {
                return double.class;
            }
        } else {
            if ( lastChar == 'l' || lastChar == 'L' ) {
                return long.class;
            } else {
                return int.class;
            }
        }
    }

    public static boolean isNumber( String text ) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Double.parseDouble( text );
            return true;
        } catch ( NumberFormatException nfe ) {
            return false;
        }
    }

    public static boolean isChar( String text ) {
        return CHAR_REGEX.matcher( text ).matches();
    }

    public static boolean isString( String text ) {
        return text.startsWith( "\"" ) && text.endsWith( "\"" );
    }

}
