package com.athaydes.osgiaas.api.cli;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * It uses a space as a separator, but takes into consideration doubly-quoted values, making the whole
     * quoted value a single argument.
     *
     * @param arguments space-separated arguments
     * @return split arguments
     */
    public static String[] breakupArguments( String arguments ) {
        return breakupArguments( arguments, -1 );
    }

    /**
     * Breaks up a command arguments into separate parts.
     * It uses a space as a separator, but takes into consideration doubly-quoted values, making the whole
     * quoted value a single argument.
     *
     * @param arguments command arguments or full command
     * @param limit     maximum number of argument parts to consider. Each part is separated by one or more whitespaces.
     * @return split arguments
     */
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
        String[] parts = breakupArguments( line, maxArgs + 1 );
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

        return new CommandInvocation( toArguments( keyValues ), unprocessed );
    }

    private static List<CommandArgument> toArguments( Map<String, List<String>> keyValues ) {
        List<CommandArgument> result = new ArrayList<>( keyValues.size() );
        keyValues.forEach( ( key, values ) -> result.add( new CommandArgument( key, values ) ) );
        return result;
    }

}
