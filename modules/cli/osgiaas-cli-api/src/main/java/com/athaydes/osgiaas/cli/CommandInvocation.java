package com.athaydes.osgiaas.cli;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of command invocation.
 */
public class CommandInvocation {

    private final Map<String, List<List<String>>> options;
    private final String unprocessedInput;

    public CommandInvocation( Map<String, List<List<String>>> options,
                              String unprocessedInput ) {
        this.options = options;
        this.unprocessedInput = unprocessedInput;
    }

    /**
     * Get a Map of the options and their arguments provided during the command invocation.
     * <p>
     * Each entry of the map consists of the option (key) provided, mapped to the option's arguments, if any.
     * <p>
     * For example, if a command accepts a '-f' option which accepts one argument, and the user enters
     * 'command -f hello', the options Map will contain {@code {'-f' -> ['hello']}}.
     * <p>
     * Anything that does not match any argument can be retrieved with the {@link #getUnprocessedInput()} method.
     *
     * @return Map of command options (option args by option).
     */
    public Map<String, List<List<String>>> getOptions() {
        return options;
    }

    /**
     * Get the first argument provided for the given option.
     * <p>
     * If no argument was given, this method returns null.
     *
     * @param option option
     * @return first argument for option, or null if none was provided.
     */
    @Nullable
    public String getOptionFirstArgument( String option ) {
        List<String> values = getAllArgumentsFor( option );
        if ( values.isEmpty() ) {
            return null;
        } else {
            return values.get( 0 );
        }
    }

    /**
     * Get the arguments provided for an option.
     * <p>
     * If the option was not specified, or it was specified but no argument was provided, this method returns
     * the empty List.
     *
     * @param option command option
     * @return argument values for the given option, or the empty List if none was provided.
     */
    public List<String> getAllArgumentsFor( String option ) {
        List<List<String>> allArgs = options.getOrDefault( option, Collections.emptyList() );
        int size = 0;
        for (List<String> list : allArgs) {
            size += list.size();
        }
        List<String> result = new ArrayList<>( size );
        for (List<String> list : allArgs) {
            result.addAll( list );
        }
        return result;
    }

    /**
     * Check if an option was provided during the command invocation.
     *
     * @param option option
     * @return whether the option was provided by the user.
     */
    public boolean hasOption( String option ) {
        return options.containsKey( option );
    }

    /**
     * Get unprocessed input, ie. the final part of the command invocation that did not match any
     * command options/arguments.
     *
     * @return unprocessed input
     */
    public String getUnprocessedInput() {
        return unprocessedInput;
    }

    @Override
    public String toString() {
        return "CommandInvocation{" +
                "options=" + options +
                ", unprocessedInput='" + unprocessedInput + '\'' +
                '}';
    }
}
