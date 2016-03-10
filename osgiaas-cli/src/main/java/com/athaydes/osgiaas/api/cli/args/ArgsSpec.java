package com.athaydes.osgiaas.api.cli.args;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandInvocation;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final boolean noFurtherArgumentsAllowed;

    private ArgsSpec( List<Arg> arguments, boolean noFurtherArgumentsAllowed ) {
        this.argMap = arguments.stream().collect( Collectors.toMap(
                arg -> arg.key,
                Function.identity() ) );
        this.noFurtherArgumentsAllowed = noFurtherArgumentsAllowed;
    }

    public CommandInvocation parse( String command ) throws IllegalArgumentException {
        AtomicReference<List<String>> latestParameterList = new AtomicReference<>();
        AtomicReference<String> abortedParam = new AtomicReference<>();
        Map<String, List<String>> result = new HashMap<>();

        String unprocessedInput = CommandHelper.breakupArguments( command, param -> {
            @Nullable List<String> latestParams = latestParameterList.getAndSet( null );
            if ( latestParams != null ) {
                latestParams.add( param );
            } else {
                @Nullable Arg arg = argMap.get( param );
                if ( arg == null ) {
                    abortedParam.set( param );
                    return false; // cannot understand this parameter
                } else if ( !arg.allowMultiple && result.containsKey( arg.key ) ) {
                    throw new IllegalArgumentException( "Duplicate argument not allowed: " + arg.key );
                }

                List<String> params = new ArrayList<>( arg.allowMultiple ? 5 : 1 );
                if ( arg.takesArgument ) {
                    latestParameterList.set( params );
                }
                result.put( arg.key, params );
            }
            return true; // continue parsing
        } );

        @Nullable String aborted = abortedParam.get();

        if ( aborted != null ) {
            if ( noFurtherArgumentsAllowed ) {
                throw new IllegalArgumentException( "Illegal argument options provided: " + aborted );
            }
            unprocessedInput = aborted + " " + unprocessedInput;
        }

        return new CommandInvocation( result, unprocessedInput );
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
        private int parametersToSkip = 0;
        private boolean noFurtherArgumentsAllowed = false;

        private ArgsSpecBuilder() {
            // use builder factory method
        }

        public ArgsSpecBuilder skip( int parametersToSkip ) {
            this.parametersToSkip = parametersToSkip;
            return this;
        }

        public ArgsSpecBuilder accepts( RegularExpression regex ) {
            return this;
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

        public ArgsSpecBuilder noFurtherArgumentsAllowed() {
            noFurtherArgumentsAllowed = true;
            return this;
        }

        public ArgsSpec build() {
            return new ArgsSpec( arguments, noFurtherArgumentsAllowed );
        }

    }

}
