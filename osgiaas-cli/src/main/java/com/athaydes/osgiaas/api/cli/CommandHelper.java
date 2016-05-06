package com.athaydes.osgiaas.api.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility that can be used to implement Felix Commands.
 */
public class CommandHelper {

    public static final int SPACE_CODE = ( int ) ' ';
    public static final int AMPERSAND_CODE = ( int ) '&';
    public static final int PIPE_CODE = ( int ) '|';
    public static final int ESCAPE_CODE = ( int ) '\\';
    public static final int DOUBLE_QUOTE_CODE = ( int ) '"';

    public static final Set<Integer> commandSeparators = Collections.unmodifiableSet( new HashSet<Integer>( 3 ) {{
        add( SPACE_CODE );
        add( AMPERSAND_CODE );
        add( PIPE_CODE );
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
     * Breaks up a command arguments into separate parts using a function to receive arguments and determine
     * when to stop.
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
        return breakupArguments( arguments, limitFunction, false, false, SPACE_CODE, DOUBLE_QUOTE_CODE );
    }

    /**
     * Breaks up a command arguments into separate parts using a function to receive arguments and determine
     * when to stop.
     * <p>
     *
     * @param arguments         command arguments or full command
     * @param includeSeparators include separators as arguments
     * @param includeQuotes     include quotes in quoted arguments
     * @param separatorCode     codepoint for the separator character
     * @param quoteCodes        codepoints for the quotation characters
     * @param limitFunction     function that receives each argument, returning true to continue breaking up the input,
     *                          or false to stop. The unprocessed input is returned.
     * @return the unprocessed input.
     */
    public static String breakupArguments( String arguments,
                                           Function<String, Boolean> limitFunction,
                                           boolean includeSeparators,
                                           boolean includeQuotes,
                                           int separatorCode,
                                           int... quoteCodes ) {
        boolean inQuote = false;
        boolean escaped = false;
        boolean inSeparators = false;
        StringBuilder currentArg = new StringBuilder();

        PrimitiveIterator.OfInt chars = arguments.codePoints().iterator();
        int index = 0;

        while ( chars.hasNext() ) {
            index++;
            int c = chars.nextInt();

            boolean isEscape = ( c == ESCAPE_CODE );

            boolean isQuote = false;
            for (int quote : quoteCodes) {
                if ( c == quote ) {
                    isQuote = true;
                    break;
                }
            }

            if ( escaped && !isQuote ) { // put back the escaping char as it was not used
                currentArg.appendCodePoint( ESCAPE_CODE );
            }

            if ( isEscape ) {
                escaped = true;
                inSeparators = false;
                continue;
            }

            boolean done = false;

            if ( !escaped && isQuote ) {
                inQuote = !inQuote;
                done = true;
                inSeparators = false;
                if ( includeQuotes ) {
                    currentArg.appendCodePoint( c );
                }
            }

            escaped = false;

            if ( done ) {
                continue;
            }

            if ( inQuote ) {
                // when in quotes, we don't care what c is
                currentArg.appendCodePoint( c );
            } else {
                // outside quotes, we need to look for the separator
                if ( c == separatorCode ) {
                    // just started separators?
                    if ( !inSeparators ) {
                        boolean keepGoing = addArgument( currentArg, limitFunction );
                        if ( !keepGoing ) {
                            // no more splitting
                            return arguments.substring( index );
                        }
                    }
                    inSeparators = true;
                    if ( includeSeparators ) {
                        currentArg.appendCodePoint( c );
                    }
                } else {
                    // check if we were in separators before if we need to add separators
                    if ( includeSeparators && inSeparators ) {
                        boolean keepGoing = addArgument( currentArg, limitFunction );
                        if ( !keepGoing ) {
                            // no more splitting
                            return arguments.substring( index );
                        }
                    }
                    inSeparators = false;
                    currentArg.appendCodePoint( c );
                }
            }
        }

        if ( escaped ) {
            // don't throw away the last escaping character
            currentArg.appendCodePoint( ESCAPE_CODE );
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
