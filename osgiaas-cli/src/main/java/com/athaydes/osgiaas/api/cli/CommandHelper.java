package com.athaydes.osgiaas.api.cli;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

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
            boolean escapeNext = !escaped && ( c == '\\' );

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
                        boolean keepGoing = addArgument( currentArg, limitFunction );
                        if ( !keepGoing ) {
                            // no more splitting
                            char[] rest = new char[ chars.length - ( i + 1 ) ];
                            System.arraycopy( chars, i + 1, rest, 0, rest.length );
                            return new String( rest );
                        }
                    } else if ( !escapeNext ) {
                        currentArg.append( c );
                    }
                }
            }

            escaped = escapeNext;
        }

        // add last argument if any
        addArgument( currentArg, limitFunction );

        return "";
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

    /**
     * Parses a command line invocation up to the given number of arguments.
     * <p>
     * Notice that the whole line is processed as arguments, so if the first part of the
     * line invocation is the command name, then the command name will be the first 'argument' returned.
     *
     * @param line    line to process
     * @param maxArgs maximum number of arguments to parse
     * @return parsed command invocation
     */
    public static CommandInvocation parseCommandInvocation( String line, int maxArgs ) {
        String[] parts = { line };// breakupArguments( line, maxArgs + 1 );
        Map<String, List<String>> keyValues = new HashMap<>();
        @Nullable String currentKey = null;

        int i = 0;
        for (; i < Math.min( parts.length, maxArgs ); i++) {
            String part = parts[ i ];

            if ( part.startsWith( "-" ) ) {
                if ( currentKey != null ) {
                    keyValues.put( currentKey, new ArrayList<>() );
                }
                currentKey = part;
            } else {
                if ( currentKey != null ) {
                    keyValues.computeIfAbsent( currentKey, ( k ) -> new ArrayList<>() )
                            .add( part );
                } else {
                    keyValues.put( part, new ArrayList<>() );
                }
                currentKey = null;
            }
        }

        if ( currentKey != null ) {
            keyValues.put( currentKey, new ArrayList<>() );
        }

        String unprocessed = "";

        if ( i < parts.length ) {
            unprocessed += Stream.of( parts )
                    .skip( i )
                    .reduce( unprocessed, String::concat );
        }

        return new CommandInvocation( keyValues, unprocessed );
    }

}
