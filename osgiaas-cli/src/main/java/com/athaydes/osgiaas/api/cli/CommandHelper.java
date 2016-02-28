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
        String currentArg = "";
        List<String> result = new ArrayList<>();
        boolean applyLimit = limit > 0;

        for (char c : arguments.toCharArray()) {
            boolean escapeNext;

            if ( applyLimit && result.size() >= limit - 1 ) {
                // no more splitting
                inQuote = true;
                escapeNext = false;
            } else {
                escapeNext = !escaped && ( c == '\\' );
            }

            if ( inQuote ) {
                if ( c == '"' && !escaped ) {
                    inQuote = false;
                } else if ( !escapeNext ) {
                    currentArg += c;
                }
            } else {
                if ( c == '"' && !escaped ) {
                    inQuote = true;
                } else {
                    if ( c == ' ' ) {
                        if ( !currentArg.isEmpty() ) {
                            result.add( currentArg );
                            currentArg = "";
                        }
                    } else if ( !escapeNext ) {
                        currentArg += c;
                    }
                }
            }

            escaped = escapeNext;
        }

        if ( !currentArg.isEmpty() ) {
            result.add( currentArg );
        }

        return result.toArray( new String[ result.size() ] );
    }

}
