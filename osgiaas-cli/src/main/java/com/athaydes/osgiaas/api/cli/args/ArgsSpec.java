package com.athaydes.osgiaas.api.cli.args;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandInvocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final Set<String> mandatoryArgs;

    private ArgsSpec( List<Arg> arguments ) {
        this.mandatoryArgs = new HashSet<>();
        Function<Arg, String> addIfMandatoryAndGetKey = ( arg ) -> {
            if ( arg.mandatory ) {
                mandatoryArgs.add( arg.key );
            }
            return arg.key;
        };

        this.argMap = arguments.stream().collect( Collectors.toMap(
                addIfMandatoryAndGetKey,
                Function.identity() ) );
    }

    public CommandInvocation parse( String command ) throws IllegalArgumentException {
        command = removeFirstPartOf( command );

        AtomicReference<String> abortedParameterRef = new AtomicReference<>();
        AtomicReference<String> currentParameterRef = new AtomicReference<>();
        Map<String, List<String>> result = new LinkedHashMap<>();

        String unprocessedInput = CommandHelper.breakupArguments( command, param -> {
            @Nullable String currentParameter = currentParameterRef.getAndSet( null );
            if ( currentParameter != null ) {
                result.get( currentParameter ).add( param );
                return true; // continue parsing
            } else {
                @Nullable Arg arg = argMap.get( param );
                if ( arg == null ) {
                    // cannot understand this parameter, stop parsing
                    abortedParameterRef.set( param );
                    return false;
                } else if ( !arg.allowMultiple && result.containsKey( arg.key ) ) {
                    throw new IllegalArgumentException( "Duplicate argument not allowed: " + arg.key );
                } else {
                    @Nullable List<String> parameters = result.get( param );
                    if ( parameters == null ) {
                        parameters = new ArrayList<>( arg.allowMultiple ? 5 : 1 );
                        result.put( arg.key, parameters );
                    }
                    if ( arg.takesArgument ) {
                        currentParameterRef.set( param );
                    }
                    return true; // continue parsing
                }
            }
        } );

        @Nullable String currentParameter = currentParameterRef.get();
        if ( currentParameter != null ) {
            throw new IllegalArgumentException( "Missing argument for parameter " + currentParameter );
        }

        Set<String> nonProvidedMandatoryArgs = new HashSet<>( mandatoryArgs );
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

    private String putBackAbortedParameter( String unprocessedInput, String abortedParameter ) {
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
            return command;
        } else {
            return command.substring( index + 1 );
        }
    }

    public static ArgsSpecBuilder builder() {
        return new ArgsSpecBuilder();
    }

    private static class Arg {
        private final String key;
        private final boolean mandatory;
        private final boolean takesArgument;
        private final boolean allowMultiple;

        public Arg( String key, boolean mandatory,
                    boolean takesArgument, boolean allowMultiple ) {
            this.key = key;
            this.mandatory = mandatory;
            this.takesArgument = takesArgument;
            this.allowMultiple = allowMultiple;
        }
    }

    public static class ArgsSpecBuilder {

        private final List<Arg> arguments = new ArrayList<>();

        private ArgsSpecBuilder() {
            // use builder factory method
        }

        public ArgsSpecBuilder accepts( String argument ) {
            return accepts( argument, false, false, false );
        }

        public ArgsSpecBuilder accepts( String argument, boolean mandatory ) {
            return accepts( argument, mandatory, false, false );
        }

        public ArgsSpecBuilder accepts( String argument, boolean mandatory,
                                        boolean takesArgument ) {
            return accepts( argument, mandatory, takesArgument, false );
        }

        public ArgsSpecBuilder accepts( String argument, boolean mandatory,
                                        boolean takesArgument, boolean allowMultiple ) {
            arguments.add( new Arg( argument, mandatory, takesArgument, allowMultiple ) );
            return this;
        }

        public ArgsSpec build() {
            return new ArgsSpec( arguments );
        }

    }

}
