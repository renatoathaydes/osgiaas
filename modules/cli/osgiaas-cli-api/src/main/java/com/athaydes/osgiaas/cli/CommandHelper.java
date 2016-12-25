package com.athaydes.osgiaas.cli;

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

    public static final int SPACE_CODE = ' ';
    public static final int AMPERSAND_CODE = '&';
    public static final int PIPE_CODE = '|';
    public static final int ESCAPE_CODE = '\\';
    public static final int DOUBLE_QUOTE_CODE = '"';
    public static final int SINGLE_QUOTE_CODE = '\'';

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
        return breakupArguments( arguments, limitFunction, CommandBreakupOptions.create() );
    }

    /**
     * Breaks up a command arguments into separate parts using the provided options for configuration.
     *
     * @param arguments command arguments or full command
     * @param options   options to customize how the arguments are broken up
     * @return the unprocessed input.
     */
    public static List<String> breakupArguments( String arguments,
                                                 CommandBreakupOptions options ) {
        List<String> result = new ArrayList<>();
        breakupArguments( arguments, result::add, options );
        return result;
    }

    /**
     * Breaks up a command arguments into separate parts using a function to receive arguments and determine
     * when to stop.
     * <p>
     *
     * @param arguments     command arguments or full command
     * @param limitFunction function that receives each argument, returning true to continue breaking up the input,
     *                      or false to stop. The unprocessed input is returned.
     * @param options       options to customize how the arguments are broken up
     * @return the unprocessed input.
     */
    public static String breakupArguments( String arguments,
                                           Function<String, Boolean> limitFunction,
                                           CommandBreakupOptions options ) {
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
            boolean isSeparator = ( c == options.separatorCode );

            boolean isQuote = false;
            for (int quote : options.quoteCodes) {
                if ( c == quote ) {
                    isQuote = true;
                    break;
                }
            }

            if ( escaped && !isQuote && !isSeparator ) { // put back the escaping char as it was not used
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
                if ( options.includeQuotes ) {
                    currentArg.appendCodePoint( c );
                }
            }

            if ( done ) {
                escaped = false;
                continue;
            }

            if ( inQuote ) {
                // when in quotes, we don't care what c is
                currentArg.appendCodePoint( c );
            } else {
                // outside quotes, we need to look for the separator
                if ( !escaped && isSeparator ) {
                    // just started separators?
                    if ( !inSeparators ) {
                        boolean keepGoing = addArgument( currentArg, limitFunction );
                        if ( !keepGoing ) {
                            // no more splitting
                            return arguments.substring( index );
                        }
                    }
                    inSeparators = true;
                    if ( options.includeSeparators ) {
                        currentArg.appendCodePoint( c );
                    }
                } else {
                    // check if we were in separators before if we need to add separators
                    if ( options.includeSeparators && inSeparators ) {
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

            escaped = false;
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
     * A separator character is either a {@code ' ', '|' or a '&'}.
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

    public static class CommandBreakupOptions {

        private boolean includeSeparators = false;
        private boolean includeQuotes = false;
        private int separatorCode = SPACE_CODE;
        private int[] quoteCodes = { DOUBLE_QUOTE_CODE };

        private CommandBreakupOptions() {
        }

        public static CommandBreakupOptions create() {
            return new CommandBreakupOptions();
        }

        public CommandBreakupOptions includeSeparators( boolean includeSeparators ) {
            this.includeSeparators = includeSeparators;
            return this;
        }

        public CommandBreakupOptions includeQuotes( boolean includeQuotes ) {
            this.includeQuotes = includeQuotes;
            return this;
        }

        public CommandBreakupOptions separatorCode( int separatorCode ) {
            this.separatorCode = separatorCode;
            return this;
        }

        public CommandBreakupOptions quoteCodes( int... quoteCodes ) {
            this.quoteCodes = quoteCodes;
            return this;
        }
    }
}
