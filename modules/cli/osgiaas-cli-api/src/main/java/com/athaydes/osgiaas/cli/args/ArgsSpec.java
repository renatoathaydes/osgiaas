package com.athaydes.osgiaas.cli.args;

import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandHelper.CommandBreakupOptions;
import com.athaydes.osgiaas.cli.CommandInvocation;

import javax.annotation.Nullable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

/**
 * Command arguments specification.
 * <p>
 * Used by CLI Commands to specify the arguments they may accept, generate documentation and a {@link CommandCompleter}
 * automatically.
 * <p>
 * To parse the command-line input, use the {@link #parse(String)} method, then the {@link CommandInvocation} object it
 * returns to access the options provided by the user.
 * <p>
 * To enable command auto-completion for a Command, export a {@code CommandCompleter} OSGi service that
 * delegates to the instance returned by {@link #getCommandCompleter(String)}).
 * <p>
 * Standard options documentation can be obtained by calling {@link #getUsage()} and {@link #getDocumentation(String)}.
 */
public class ArgsSpec {

    private final Map<String, Arg> argMap;
    private final Set<String> mandatoryArgKeys;
    private final boolean showEnumeratedValuesInDocumentation;

    private ArgsSpec( List<Arg> arguments, boolean showEnumeratedValuesInDocumentation ) {
        this.showEnumeratedValuesInDocumentation = showEnumeratedValuesInDocumentation;

        Set<String> tempMandatoryArgs = new HashSet<>(
                ( int ) arguments.stream().filter( arg -> arg.mandatory ).count() );

        Map<String, Arg> tempArgMap = new LinkedHashMap<>( arguments.size() );

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

    public CommandCompleter getCommandCompleter( String commandName ) {
        return new ArgsCommandCompleter( commandName );
    }

    /**
     * @return the supported options in a format suitable for command usage documentation.
     */
    public String getUsage() {
        return argMap.values().stream()
                .distinct()
                .map( this::usageFor )
                .collect( joining( " " ) );
    }

    private String usageFor( Arg arg ) {
        StringBuilder builder = new StringBuilder();

        if ( !arg.mandatory ) {
            builder.append( '[' );
        }

        appendOption( builder, arg.key, true, arg.allowMultiple );
        appendArguments( builder, arg );

        if ( !arg.mandatory ) {
            builder.append( ']' );
        }

        return builder.toString();
    }

    private void appendOption( StringBuilder builder, String option, boolean mandatory, boolean allowMultiple ) {
        if ( !mandatory ) {
            builder.append( '[' );
        }

        builder.append( option );

        if ( allowMultiple ) {
            builder.append( '…' );
        }
        if ( !mandatory ) {
            builder.append( ']' );
        }
    }

    private void appendArguments( StringBuilder builder, Arg arg ) {
        Function<Map.Entry<String, Supplier<? extends Collection<String>>>, String> doc = ( entry ) -> {
            if ( !showEnumeratedValuesInDocumentation ) {
                return entry.getKey();
            }
            Collection<String> enumeratedValues = entry.getValue().get();
            if ( enumeratedValues.isEmpty() ) {
                return entry.getKey();
            } else {
                return String.join( "|", enumeratedValues );
            }
        };

        if ( arg.mandatoryArgs.size() > 0 ) {
            builder.append( ' ' );
            builder.append( arg.mandatoryArgs.stream()
                    .map( it -> "<" + doc.apply( it ) + ">" )
                    .collect( joining( " " ) ) );
        }

        if ( arg.optionalArgs.size() > 0 ) {
            builder.append( " [" )
                    .append( arg.optionalArgs.stream()
                            .map( it -> "<" + doc.apply( it ) + ">" )
                            .collect( joining( " " ) ) )
                    .append( ']' );
        }
    }

    /**
     * @return documentation for the specified options.
     */
    public String getDocumentation() {
        return getDocumentation( "" );
    }

    /**
     * @param indentation to prepend on each line
     * @return documentation for the specified options.
     */
    public String getDocumentation( String indentation ) {
        Function<Arg, String> documentArg = ( arg ) -> {
            StringBuilder builder = new StringBuilder();
            builder.append( indentation ).append( "* " );

            appendOption( builder, arg.key, arg.mandatory, arg.allowMultiple );

            if ( arg.longKey != null ) {
                builder.append( ", " );
                appendOption( builder, arg.longKey, arg.mandatory, arg.allowMultiple );
            }

            appendArguments( builder, arg );

            if ( arg.description != null ) {
                builder.append( "\n" ).append( indentation ).append( indentation ).append( arg.description );
            }

            return builder.toString();
        };

        return argMap.values().stream()
                .distinct()
                .map( documentArg )
                .collect( joining( "\n" ) );
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
        private final List<Map.Entry<String, Supplier<? extends Collection<String>>>> mandatoryArgs;
        private final List<Map.Entry<String, Supplier<? extends Collection<String>>>> optionalArgs;
        private final int minArgs;
        private final int maxArgs;
        private final boolean allowMultiple;

        private Arg( String key, @Nullable String longKey, @Nullable String description, boolean mandatory,
                     List<Map.Entry<String, Supplier<? extends Collection<String>>>> mandatoryArgs,
                     List<Map.Entry<String, Supplier<? extends Collection<String>>>> optionalArgs,
                     boolean allowMultiple ) {
            this.key = key;
            this.longKey = longKey;
            this.description = description;
            this.mandatory = mandatory;
            this.mandatoryArgs = mandatoryArgs;
            this.optionalArgs = optionalArgs;
            this.allowMultiple = allowMultiple;

            this.minArgs = mandatoryArgs.size();
            this.maxArgs = mandatoryArgs.size() + optionalArgs.size();
        }
    }

    /**
     * Builder of {@link ArgsSpec} instances.
     */
    public static class ArgsSpecBuilder {

        private final List<Arg> arguments = new ArrayList<>();
        private boolean showEnumeratedValuesInDocumentation = false;

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
         * Show the enumerated values for arguments specified with {@link ArgBuilder#withEnumeratedArgs(Map)} or
         * {@link ArgBuilder#withOptionalEnumeratedArgs(Map)} in the generated documentation.
         * <p>
         * By default, the name of the argument is shown, not its enumerated values.
         * <p>
         * Notice that because the enumerated values are provided lazily, a different set of values could be
         * returned each time the documentation is generated. It is advisable to only use this option in case
         * the enumerated argument values are completely static.
         *
         * @return this builder
         */
        public ArgsSpecBuilder showEnumeratedArgValuesInDocumentation() {
            showEnumeratedValuesInDocumentation = true;
            return this;
        }

        /**
         * @return the argument specification
         */
        public ArgsSpec build() {
            return new ArgsSpec( arguments, showEnumeratedValuesInDocumentation );
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
            private List<Map.Entry<String, Supplier<? extends Collection<String>>>> mandatoryArgs = new ArrayList<>( 2 );
            private List<Map.Entry<String, Supplier<? extends Collection<String>>>> optionalArgs = new ArrayList<>( 2 );
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
             * Set mandatory named arguments the option must take.
             * <p>
             * The named arguments are used both to know how many arguments to parse and for documentation.
             *
             * @param args named arguments
             * @return this builder
             */
            public ArgBuilder withArgs( String... args ) {
                for (String arg : args) {
                    mandatoryArgs.add( new SimpleEntry<>( arg, Collections::emptyList ) );
                }
                return this;
            }

            /**
             * Set mandatory named arguments, with enumerated possible values, the option must take.
             * <p>
             * The named arguments are used both to know how many arguments to parse and for documentation.
             * The enumerated values for each argument are used for CLI auto-completion. If the supplied List for an
             * argument is empty, then no auto-completion is possible.
             * <p>
             * Enumerated values are provided via a #Supplier, meaning they are evaluated each time, making it
             * possible to support dynamic arguments such as file names within a directory.
             *
             * @param name                     argument name (used only for documentation)
             * @param enumeratedValuesSupplier possible values the
             *                                 argument might take (used for auto-completion).
             * @return this builder
             */
            public ArgBuilder withEnumeratedArg( String name, Supplier<? extends Collection<String>> enumeratedValuesSupplier ) {
                mandatoryArgs.add( new SimpleEntry<>( name, enumeratedValuesSupplier ) );
                return this;
            }

            /**
             * Set mandatory named arguments, with enumerated possible values, the option must take.
             * <p>
             * The named arguments are used both to know how many arguments to parse and for documentation.
             * The enumerated values for each argument are used for CLI auto-completion. If the supplied List for an
             * argument is empty, then no auto-completion is possible.
             * <p>
             * Enumerated values are provided via a #Supplier, meaning they are evaluated each time, making it
             * possible to support dynamic arguments such as file names within a directory.
             *
             * @param enumeratedArgs map from argument name (used only for documentation) to possible values the
             *                       argument might take (used for auto-completion).
             * @return this builder
             */
            public ArgBuilder withEnumeratedArgs( Map<String, Supplier<? extends Collection<String>>> enumeratedArgs ) {
                mandatoryArgs.addAll( enumeratedArgs.entrySet() );
                return this;
            }

            /**
             * Set the optional named arguments the option might take.
             * <p>
             * The named arguments are used both to know how many arguments to parse and for documentation.
             *
             * @param args named arguments
             * @return this builder
             */
            public ArgBuilder withOptionalArgs( String... args ) {
                for (String arg : args) {
                    optionalArgs.add( new SimpleEntry<>( arg, Collections::emptyList ) );
                }
                return this;
            }

            /**
             * Set optional named arguments, with enumerated possible values, the option might take.
             * <p>
             * The named arguments are used both to know how many arguments to parse and for documentation.
             * The enumerated values for each argument are used for CLI auto-completion. If the supplied List for an
             * argument is empty, then no auto-completion is possible.
             * <p>
             * Enumerated values are provided via a #Supplier, meaning they are evaluated each time, making it
             * possible to support dynamic arguments such as file names within a directory.
             *
             * @param enumeratedArgs map from argument name (used only for documentation) to possible values the
             *                       argument might take (used for auto-completion).
             * @return this builder
             */
            public ArgBuilder withOptionalEnumeratedArgs( Map<String, Supplier<? extends Collection<String>>> enumeratedArgs ) {
                optionalArgs.addAll( enumeratedArgs.entrySet() );
                return this;
            }

            /**
             * Set optional named arguments, with enumerated possible values, the option might take.
             * <p>
             * The named arguments are used both to know how many arguments to parse and for documentation.
             * The enumerated values for each argument are used for CLI auto-completion. If the supplied List for an
             * argument is empty, then no auto-completion is possible.
             * <p>
             * Enumerated values are provided via a #Supplier, meaning they are evaluated each time, making it
             * possible to support dynamic arguments such as file names within a directory.
             *
             * @param name                     of the argument (used only for documentation)
             * @param enumeratedValuesSupplier possible values the
             *                                 argument might take (used for auto-completion).
             * @return this builder
             */
            public ArgBuilder withOptionalEnumeratedArg( String name, Supplier<List<String>> enumeratedValuesSupplier ) {
                optionalArgs.add( new SimpleEntry<>( name, enumeratedValuesSupplier ) );
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
                arguments.add( new Arg( option, longOption, description, mandatory,
                        mandatoryArgs, optionalArgs, allowMultiple ) );
                return ArgsSpecBuilder.this;
            }

        }

    }

    private class ArgsCommandCompleter implements CommandCompleter {

        private final String commandName;

        private ArgsCommandCompleter( String commandName ) {
            this.commandName = commandName;
        }

        @Override
        public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
            List<String> commandParts = CommandHelper.breakupArguments( buffer.substring( 0, cursor ),
                    CommandBreakupOptions.create().includeSeparators( true ) );

            if ( commandParts.size() < 2 || !commandParts.get( 0 ).equals( commandName ) ) {
                return -1;
            }

            String lastPart = commandParts.remove( commandParts.size() - 1 );
            int lastPartLength = lastPart.length();
            lastPart = lastPart.trim();

            Set<Arg> remainingOptions = new HashSet<>( argMap.values() );
            Set<Arg> specifiedOptions = new HashSet<>();
            int completionIndex = lastPart.isEmpty() ? lastPartLength : 0;
            List<? extends Collection<String>> currentMandatoryArgs = new ArrayList<>();

            // treat the command itself as the first argument (add null because we can't add
            // anything else to a generic list without its concrete type)
            currentMandatoryArgs.add( null );

            List<? extends Collection<String>> currentOptionalArgs = new ArrayList<>();

            for (String commandPart : commandParts) {
                // to keep track of the current position, we must use the un-trimmed command part's length
                completionIndex += commandPart.length();

                // but after that is done, we can trim the command part
                commandPart = commandPart.trim();

                if ( commandPart.isEmpty() ) {
                    continue;
                }

                if ( currentMandatoryArgs.size() > 0 ) {
                    Collection<String> options = emptyListOr( currentMandatoryArgs.remove( 0 ) );
                    if ( !options.isEmpty() && !options.contains( commandPart ) ) {
                        // mandatory argument value was not matched
                        return -1;
                    }

                    // mandatory argument value matched
                    continue;
                }

                if ( !currentOptionalArgs.isEmpty() ) {
                    Collection<String> options = currentOptionalArgs.remove( 0 );
                    if ( options.isEmpty() || options.contains( commandPart ) ) {
                        // this is an optional argument
                        continue;
                    }
                }

                @Nullable Arg arg = argMap.get( commandPart );

                boolean noMatch =
                        // no argument found
                        arg == null ||
                                // multiple times not allowed and already specified
                                ( !arg.allowMultiple && specifiedOptions.contains( arg ) );

                if ( !noMatch ) {
                    // we got a matching arg
                    specifiedOptions.add( arg );

                    if ( !arg.allowMultiple ) {
                        remainingOptions.remove( arg );
                    }

                    Function<List<Entry<String, Supplier<? extends Collection<String>>>>,
                            List<? extends Collection<String>>> optionArgValues =
                            ( a ) -> a.stream()
                                    .map( Entry::getValue )
                                    .map( Supplier::get )
                                    .collect( Collectors.toList() );

                    currentMandatoryArgs = optionArgValues.apply( arg.mandatoryArgs );
                    currentOptionalArgs = optionArgValues.apply( arg.optionalArgs );
                } else {
                    // did not match anything
                    return -1;
                }
            }

            boolean foundCompletion = false;

            if ( !currentMandatoryArgs.isEmpty() ) {
                // mandatory arguments are required, use only valid options for completion
                Collection<String> possibleCompletions = emptyListOr( currentMandatoryArgs.get( 0 ) );
                for (String possibility : possibleCompletions) {
                    if ( possibility.startsWith( lastPart ) ) {
                        candidates.add( possibility );
                        foundCompletion = true;
                    }
                }

                // only check for more possibilities if no mandatory argument was required
            } else {
                // all remaining option arguments could be used for completion
                if ( !currentOptionalArgs.isEmpty() ) {
                    Collection<String> possibleCompletions = currentOptionalArgs.get( 0 );
                    for (String possibility : possibleCompletions) {
                        if ( possibility.startsWith( lastPart ) ) {
                            candidates.add( possibility );
                            foundCompletion = true;
                        }
                    }
                }

                // all remaining options could be used for completion
                for (Arg arg : remainingOptions) {
                    if ( arg.key.startsWith( lastPart ) ) {
                        foundCompletion = true;
                        candidates.add( arg.key );
                    }
                    if ( arg.longKey != null && arg.longKey.startsWith( lastPart ) ) {
                        foundCompletion = true;
                        candidates.add( arg.longKey );
                    }
                }
            }


            if ( foundCompletion ) {
                candidates.sort( comparing( CharSequence::toString ) );
                return completionIndex;
            } else {
                return -1;
            }
        }

    }

    private static Collection<String> emptyListOr( @Nullable Collection<String> collection ) {
        if ( collection == null ) {
            return Collections.emptyList();
        } else {
            return collection;
        }
    }


}
