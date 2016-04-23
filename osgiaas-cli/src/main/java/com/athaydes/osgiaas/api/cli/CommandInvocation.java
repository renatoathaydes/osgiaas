package com.athaydes.osgiaas.api.cli;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of command invocation.
 */
public class CommandInvocation {

    private final Map<String, List<String>> arguments;
    private final String unprocessedInput;

    public CommandInvocation( Map<String, List<String>> arguments,
                              String unprocessedInput ) {
        this.arguments = arguments;
        this.unprocessedInput = unprocessedInput;
    }

    public Map<String, List<String>> getArguments() {
        return arguments;
    }

    @Nullable
    public String getArgValue( String argument ) {
        List<String> values = getAllArgValues( argument );
        if ( values.isEmpty() ) {
            return null;
        } else {
            return values.get( 0 );
        }
    }

    public List<String> getAllArgValues( String argument ) {
        return arguments.getOrDefault( argument, Collections.emptyList() );
    }

    public boolean hasArg( String argument ) {
        return arguments.containsKey( argument );
    }

    public String getUnprocessedInput() {
        return unprocessedInput;
    }

}
