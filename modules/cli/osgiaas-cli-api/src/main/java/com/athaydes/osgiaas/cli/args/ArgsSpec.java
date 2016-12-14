package com.athaydes.osgiaas.cli.args;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandHelper.CommandBreakupOptions;
import com.athaydes.osgiaas.cli.CommandInvocation;

import javax.annotation.Nullable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Command arguments specification.
 * <p>
 * Used by cli commands to specify the arguments they may accept.
 */
public class ArgsSpec {

    private final Map<String, Arg> argMap;
    private final Set<String> mandatoryArgKeys;

    private ArgsSpec( List<Arg> arguments ) {
        Set<String> tempMandatoryArgs = new HashSet<>();
        Map<String, Arg> tempArgMap = new HashMap<>();

        for (Arg arg : arguments) {
            if ( arg.mandatory ) {
                tempMandatoryArgs.add( arg.key );
            }

            tempArgMap.put( arg.key, arg );

            if ( arg.longKey != null ) {
                tempArgMap.put( arg.longKey, arg );
            }
        }

        this.argMap = Collections.unmodifiableMap( tempArgMap );
        this.mandatoryArgKeys = Collections.unmodifiableSet( tempMandatoryArgs );
    }

    /**
     * @return documentation for the specified options.
     */
    public String getDocumentation() {
        Function<Arg, String> documentArg = ( arg ) -> {
            StringBuilder builder = new StringBuilder();
            builder.append( "* " ).append( arg.key );
            if ( arg.longKey != null ) {
                builder.append( ", " ).append( arg.longKey );
            }
            if ( arg.description != null ) {
                builder.append( "\n" ).append( "  " ).append( arg.description );
            }
            return builder.toString();
        };

        return argMap.entrySet().stream().sorted( Comparator.comparing( a -> a.getValue().key ) )
                .map( Entry::getValue )
                .map( documentArg )
                .collect( Collectors.joining( "\n" ) );
    }

    /**
     * Parse the given command, returning a {@link CommandInvocation} instance.
     *
     * @param command to parse
     * @return invocation object used to interpret the command
     * @throws IllegalArgumentException if the command fails to meet this arguments specification.
     */
    public CommandInvocation parse( String command ) throws IllegalArgumentException {
        return parse( command, CommandBreakupOptions.create() );
    }

    /**
     * Parse the given command using the provided options to break it up into tokens,
     * returning a {@link CommandInvocation} instance.
     *
     * @param command to parse
     * @param options to break up the command into tokens
     * @return invocation object used to interpret the command
     * @throws IllegalArgumentException if the command fails to meet this arguments specification.
     */
    public CommandInvocation parse( String command,
                                    CommandBreakupOptions options )
            throws IllegalArgumentException {
        command = removeFirstPartOf( command );

        AtomicReference<String> abortedParameterRef = new AtomicReference<>();
        AtomicReference<Entry<Arg, List<String>>> currentArgRef = new AtomicReference<>();
        Map<String, List<List<String>>> result = new LinkedHashMap<>();

        String unprocessedInput = CommandHelper.breakupArguments( command, param -> {
            @Nullable Entry<Arg, List<String>> previousArgEntry = currentArgRef.get();
            boolean isArgument = previousArgEntry != null;
            if ( isArgument ) {
                // if the maximum number of arguments were already taken, stop taking arguments
                int argumentsTaken = previousArgEntry.getValue().size();
                int maxArgs = previousArgEntry.getKey().maxArgs;

                if ( argumentsTaken >= maxArgs ) {
                    isArgument = false;
                } else {
                    // if enough arguments were taken, we may start parsing another argument
                    int minRequiredArgs = previousArgEntry.getKey().minArgs;
                    if ( argumentsTaken >= minRequiredArgs ) {
                        isArgument = !argMap.containsKey( param );
                    }
                }
            }

            if ( isArgument ) {
                previousArgEntry.getValue().add( param );
                return true; // continue parsing
            } else {
                currentArgRef.set( null );
                // before starting to parse a new option, check if the previous option got enough arguments
                if ( previousArgEntry != null ) {
                    throwIfNotEnoughArgumentsTaken( previousArgEntry );
                }
                @Nullable Arg arg = argMap.get( param );
                if ( arg == null ) {
                    // cannot understand this parameter, stop parsing
                    abortedParameterRef.set( param );
                    return false;
                } else if ( !arg.allowMultiple && result.containsKey( arg.key ) ) {
                    throw new IllegalArgumentException( "Duplicate argument not allowed: " + arg.key );
                } else {
                    // start parsing an option
                    List<String> optionArguments = new ArrayList<>( arg.maxArgs );
                    @Nullable List<List<String>> allArguments = result.get( arg.key );

                    //noinspection Java8ReplaceMapGet
                    if ( allArguments == null ) {
                        allArguments = new ArrayList<>( arg.allowMultiple ? 4 : 1 );
                        result.put( arg.key, allArguments );
                    }

                    allArguments.add( optionArguments );

                    if ( arg.minArgs > 0 ) {
                        currentArgRef.set( new SimpleEntry<>( arg, optionArguments ) );
                    }
                    return true; // continue parsing
                }
            }
        }, options );

        // check if enough arguments were taken for the last parameter entry
        @Nullable Entry<Arg, List<String>> currentParameterEntry = currentArgRef.get();
        if ( currentParameterEntry != null ) {
            throwIfNotEnoughArgumentsTaken( currentParameterEntry );
        }

        Set<String> nonProvidedMandatoryArgs = new HashSet<>( mandatoryArgKeys );
        nonProvidedMandatoryArgs.removeAll( result.keySet() );
        if ( !nonProvidedMandatoryArgs.isEmpty() ) {
            throw new IllegalArgumentException( "Mandatory arguments not provided: " +
                    nonProvidedMandatoryArgs );
        }

        @Nullable String abortedParameter = abortedParameterRef.get();
        if ( abortedParameter != null ) {
            unprocessedInput = putBackAbortedParameter( unprocessedInput, abortedParameter );
        }

        return new CommandInvocation( result, unprocessedInput );
    }

    private static void throwIfNotEnoughArgumentsTaken( Entry<Arg, List<String>> previousArgEntry ) {
        int argumentsTaken = previousArgEntry.getValue().size();
        int minRequiredArgs = previousArgEntry.getKey().minArgs;
        if ( argumentsTaken < minRequiredArgs ) {
            throw new IllegalArgumentException( String.format( "Missing argument for option %s. " +
                            "Minimum arguments: %d, got %d",
                    previousArgEntry.getKey().key, minRequiredArgs, argumentsTaken ) );
        }
    }

    private static String putBackAbortedParameter( String unprocessedInput, String abortedParameter ) {
        StringBuilder unprocessedInputBuilder = new StringBuilder(
                abortedParameter.length() + unprocessedInput.length() + 1 );

        unprocessedInputBuilder.append( abortedParameter );

        if ( !unprocessedInput.isEmpty() ) {
            unprocessedInputBuilder.append( ' ' ).append( unprocessedInput );
        }

        return unprocessedInputBuilder.toString();
    }

    private static String removeFirstPartOf( String command ) {
        int index = command.indexOf( ' ' );
        if ( index < 0 ) {
            return "";
        } else {
            return command.substring( index + 1 );
        }
    }

    /**
     * Create a builder of {@link ArgsSpec} instances.
     *
     * @return a builder.
     */
    public static ArgsSpecBuilder builder() {
        return new ArgsSpecBuilder();
    }

    private static class Arg {
        private final String key;
        @Nullable
        private final String longKey;
        @Nullable
        private final String description;
        private final boolean mandatory;
        private final int minArgs;
        private final int maxArgs;
        private final boolean allowMultiple;

        private Arg( String key, @Nullable String longKey, @Nullable String description, boolean mandatory,
                     int minArgs, int maxArgs, boolean allowMultiple ) {
            if ( minArgs < 0 || maxArgs < 0 ) {
                throw new IllegalArgumentException( "Invalid argument count range. " +
                        "Must not contain negative limits: " + minArgs + ", " + maxArgs );
            }
            this.key = key;
            this.longKey = longKey;
            this.description = description;
            this.mandatory = mandatory;
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
            this.allowMultiple = allowMultiple;
        }
    }

    /**
     * Builder of {@link ArgsSpec} instances.
     */
    public static class ArgsSpecBuilder {

        private final List<Arg> arguments = new ArrayList<>();

        private ArgsSpecBuilder() {
            // use builder factory method
        }

        /**
         * An option a command might accept.
         * <p>
         * Call {@code end()} once all options have been set to retrieve the {@link ArgsSpecBuilder}.
         *
         * @param option of the command being specified
         * @return this builder
         */
        public ArgBuilder accepts( String option ) {
            return new ArgBuilder( option );
        }

        /**
         * An option a command might accept (in short and long form).
         * <p>
         * Call {@code end()} once all options have been set to retrieve the {@link ArgsSpecBuilder}.
         *
         * @param option     of the command being specified
         * @param longOption long form of the given option
         * @return this builder
         */
        public ArgBuilder accepts( String option, String longOption ) {
            return new ArgBuilder( option, longOption );
        }

        /**
         * @return the argument specification
         */
        public ArgsSpec build() {
            return new ArgsSpec( arguments );
        }

        /**
         * Builder of single arguments for a command.
         */
        public class ArgBuilder {

            private final String option;
            @Nullable
            private final String longOption;
            @Nullable
            private String description;
            private boolean mandatory = false;
            private int minArgs = 0;
            private int maxArgs = 0;
            private boolean allowMultiple = false;

            private ArgBuilder( String option ) {
                this( option, null );
            }

            private ArgBuilder( String option, @Nullable String longOption ) {
                this.option = option;
                this.longOption = longOption;
            }

            /**
             * Provide a description for this option, so that it can be added to its documentation.
             *
             * @param description of this option
             * @return this builder
             */
            public ArgBuilder withDescription( String description ) {
                this.description = description;
                return this;
            }

            /**
             * Make this option mandatory.
             *
             * @return this builder
             */
            public ArgBuilder mandatory() {
                mandatory = true;
                return this;
            }

            /**
             * Set the exact number of arguments the option must take.
             * <p>
             * If the number of arguments the option might take is in a range of values,
             * use the {@link #withArgCount(int, int)} method.
             *
             * @param args exact number of arguments
             * @return this builder
             */
            public ArgBuilder withArgCount( int args ) {
                return withArgCount( args, args );
            }

            /**
             * Set a range of the number of arguments the option must take.
             * <p>
             * If the number of arguments the option might take is exact,
             * use the {@link #withArgCount(int)} method.
             *
             * @param minArgs minimum number of arguments
             * @param maxArgs maximum number of arguments
             * @return this builder
             */
            public ArgBuilder withArgCount( int minArgs, int maxArgs ) {
                if ( minArgs > maxArgs ) {
                    throw new IllegalArgumentException( "minArgs > maxArgs" );
                }
                if ( minArgs < 0 || maxArgs < 0 ) {
                    throw new IllegalArgumentException( "minArgs or maxArgs < 0" );
                }
                this.minArgs = minArgs;
                this.maxArgs = maxArgs;
                return this;
            }

            /**
             * Allow this option to be specified multiple times.
             * <p>
             * If not set, passing an argument more than once is considered an error.
             *
             * @return this builder
             */
            public ArgBuilder allowMultiple() {
                allowMultiple = true;
                return this;
            }

            /**
             * End the specification of this option.
             * <p>
             * To finalize the arguments specification, call {@link ArgsSpecBuilder#build()}.
             *
             * @return the {@link ArgsSpecBuilder} currently being used to specify a command's arguments.
             */
            public ArgsSpecBuilder end() {
                arguments.add( new Arg( option, longOption, description, mandatory, minArgs, maxArgs, allowMultiple ) );
                return ArgsSpecBuilder.this;
            }

        }

    }

}
