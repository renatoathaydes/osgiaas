package com.athaydes.osgiaas.api.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility that can be used to implement Felix Commands.
 */
public class CommandHelper {

    public static final Set<Integer> commandSeparators = Collections.unmodifiableSet( new HashSet<Integer>( 3 ) {{
        add( ( int ) ' ' );
        add( ( int ) '&' );
        add( ( int ) '|' );
    }} );

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
     * A whitespace is used as a separator, taking into consideration doubly-quoted values, making the whole
     * quoted value a single argument.
     *
     * @param arguments space-separated arguments
     * @return split arguments
     */
    public static List<String> breakupArguments( String arguments ) {
        List<String> result = new ArrayList<>();
        breakupArguments( arguments, arg -> {
            result.add( arg );
            return true;
        } );
        return result;
    }

    /**
     * Breaks up a command arguments into separate parts, up to the given limit number of parts.
     * <p>
     * A whitespace is used as a separator, taking into consideration doubly-quoted values, making the whole
     * quoted value a single argument.
     *
     * @param arguments command arguments or full command
     * @param limit     maximum number of parts to return
     * @return split arguments
     */
    public static List<String> breakupArguments( String arguments, int limit ) {
        if ( limit < 2 ) {
            return arguments.isEmpty() ?
                    Collections.emptyList() :
                    Collections.singletonList( arguments );
        }

        List<String> result = new ArrayList<>();
        int maxSize = limit - 1;
        String rest = breakupArguments( arguments, arg -> {
            result.add( arg );
            return result.size() < maxSize;
        } );

        if ( !rest.isEmpty() ) {
            result.add( rest );
        }

        return result;
    }

    /**
     * Breaks up a command arguments into separate parts using a function receive arguments and determine when to stop.
     * <p>
     * A whitespace is used as a separator, taking into consideration doubly-quoted values, making the whole
     * quoted value a single argument.
     *
     * @param arguments     command arguments or full command
     * @param limitFunction function that receives each argument, returning true to continue breaking up the input,
     *                      or false to stop. The unprocessed input is returned.
     * @return the unprocessed input.
     */
    public static String breakupArguments( String arguments, Function<String, Boolean> limitFunction ) {
        boolean inQuote = false;
        boolean escaped = false;
        StringBuilder currentArg = new StringBuilder();

        char[] chars = arguments.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[ i ];

            boolean isEscape = ( c == '\\' );
            boolean isQuote = ( c == '"' );

            if ( escaped && !isQuote ) { // put back the escaping char as it was not used
                currentArg.append( '\\' );
            }

            if ( isEscape ) {
                escaped = true;
                continue;
            }

            boolean done = false;

            if ( !escaped && isQuote ) {
                inQuote = !inQuote;
                done = true;
            }

            escaped = false;

            if ( done ) {
                continue;
            }

            if ( inQuote ) {
                // when in quotes, we don't care what c is
                currentArg.append( c );
            } else {
                // outside quotes, we need to look for whitespace
                if ( c == ' ' ) {
                    boolean keepGoing = addArgument( currentArg, limitFunction );
                    if ( !keepGoing ) {
                        // no more splitting
                        char[] rest = new char[ chars.length - ( i + 1 ) ];
                        System.arraycopy( chars, i + 1, rest, 0, rest.length );
                        return new String( rest );
                    }
                } else {
                    currentArg.append( c );
                }
            }
        }

        if ( escaped ) {
            // don't throw away the last escaping character
            currentArg.append( '\\' );
        }

        // add last argument if any
        addArgument( currentArg, limitFunction );

        return "";
    }

    /**
     * Find the index of the last command separator character.
     * <p>
     * A separator character is either a ' ', '|' or a '&'.
     *
     * @param line command line
     * @return index of the last command separator character, or -1 if not found.
     */
    public static int lastSeparatorIndex( String line ) {
        int index = line.length() - 1;

        for (; index >= 0; index--) {
            if ( commandSeparators.contains( line.codePointAt( index ) ) ) {
                return index;
            }
        }

        return -1;
    }

    private static boolean addArgument( StringBuilder currentArg,
                                        Function<String, Boolean> limitFunction ) {
        if ( currentArg.length() > 0 ) {
            String arg = currentArg.toString();
            currentArg.delete( 0, currentArg.length() );
            return limitFunction.apply( arg );
        } else {
            return true;
        }
    }
}
