package com.athaydes.osgiaas.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result of a command invocation.
 * <p>
 * Instances of this class are usually obtained by calling {@link com.athaydes.osgiaas.cli.args.ArgsSpec#parse(String)}.
 * This method attempts to parse as much as possible of the provided input, leaving any unrecognized part of the input
 * accessible with the {@link #getUnprocessedInput()} method.
 * <p>
 * Specified options can be accessed easily with {@link #getAllArgumentsFor(String)},
 * {@link #getOptionalFirstArgument(String)} or {@link #getFirstArgument(String)}.
 * <p>
 * To find out if an option was specified at all, use the {@link #hasOption(String)} method.
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
     * 'command -f hello', the options Map will contain {@code {'-f' -> [['hello']]}}.
     * <p>
     * If a command accepts a '-g' option which accepts two arguments and may be specified multiple times,
     * and the user enters 'command -g a b -g c d', the options Map will contain
     * {@code {'-g' -> [['a', 'b'], ['c', 'd']]}}.
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
     * If no argument was given, this method throws an {@link RuntimeException}.
     * <p>
     * This method should be used to get the value of options which are specified as mandatory and that take at least
     * one argument, and preferably not allowed multiple times
     * (as in that case one would be interested in all values, not just the first).
     *
     * @param option option
     * @return first argument for option.
     * @throws RuntimeException if the option was not specified. In practice, this can only occur if this option was
     *                          not marked as mandatory when the {@link com.athaydes.osgiaas.cli.args.ArgsSpec}
     *                          instance was built.
     */
    public String getFirstArgument( String option ) {
        List<String> values = getAllArgumentsFor( option );
        if ( values.isEmpty() ) {
            throw new RuntimeException( "No value specified for option: " + option );
        } else {
            return values.get( 0 );
        }
    }

    /**
     * Get the first argument provided for the given option, if any.
     * <p>
     * If no argument was given, this method returns {@link Optional#empty()}.
     *
     * @param option option
     * @return first argument for option, if it was provided.
     */
    public Optional<String> getOptionalFirstArgument( String option ) {
        List<String> values = getAllArgumentsFor( option );
        if ( values.isEmpty() ) {
            return Optional.empty();
        } else {
            return Optional.ofNullable( values.get( 0 ) );
        }
    }

    /**
     * Get the arguments provided for an option.
     * <p>
     * If the option was not specified, or it was specified but no argument was provided, this method returns
     * the empty List.
     * <p>
     * If the option was specified multiple times, the multiple argument lists provided are merged.
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
