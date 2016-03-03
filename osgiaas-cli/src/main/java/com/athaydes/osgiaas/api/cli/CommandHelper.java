package com.athaydes.osgiaas.api.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility that can be used to implement Felix Commands.
 */
public class CommandHelper {

    /**
     * Prints an error message in a standard way.
     *
     * @param err     err stream
     * @param usage   command usage
     * @param message error message
     */
    public static void printError( PrintStream err, String usage, String message ) {
        err.println( message );
        err.println( "Usage: " + usage );
    }

    /**
     * Breaks up a command arguments into separate parts.
     * It uses a space as a separator, but takes into consideration doubly-quoted values, making the whole
     * quoted value a single argument.
     *
     * @param arguments space-separated arguments
     * @return split arguments
     */
    public static String[] breakupArguments( String arguments ) {
        return breakupArguments( arguments, -1 );
    }

    public static String[] breakupArguments( String arguments, int limit ) {
        boolean inQuote = false;
        boolean escaped = false;
        StringBuilder currentArg = new StringBuilder();
        List<String> result = new ArrayList<>();
        boolean applyLimit = limit > 0;

        char[] chars = arguments.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[ i ];
            boolean escapeNext;

            if ( applyLimit && result.size() >= limit - 1 ) {
                // no more splitting
                char[] rest = new char[ chars.length - i ];
                System.arraycopy( chars, i, rest, 0, rest.length );
                result.add( new String( rest ) );
                break;
            } else {
                escapeNext = !escaped && ( c == '\\' );
            }

            if ( inQuote ) {
                if ( c == '"' && !escaped ) {
                    inQuote = false;
                } else if ( !escapeNext ) {
                    currentArg.append( c );
                }
            } else {
                if ( c == '"' && !escaped ) {
                    inQuote = true;
                } else {
                    if ( c == ' ' ) {
                        if ( currentArg.length() > 0 ) {
                            result.add( currentArg.toString() );
                            currentArg.delete( 0, currentArg.length() );
                        }
                    } else if ( !escapeNext ) {
                        currentArg.append( c );
                    }
                }
            }

            escaped = escapeNext;
        }

        if ( currentArg.length() > 0 ) {
            result.add( currentArg.toString() );
        }

        return result.toArray( new String[ result.size() ] );
    }

}
