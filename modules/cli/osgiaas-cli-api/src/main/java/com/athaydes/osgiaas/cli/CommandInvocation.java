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

    private final Map<String, List<List<String>>> arguments;
    private final String unprocessedInput;

    public CommandInvocation( Map<String, List<List<String>>> arguments,
                              String unprocessedInput ) {
        this.arguments = arguments;
        this.unprocessedInput = unprocessedInput;
    }

    public Map<String, List<List<String>>> getArguments() {
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
        List<List<String>> allArgs = arguments.getOrDefault( argument, Collections.emptyList() );
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

    public boolean hasArg( String argument ) {
        return arguments.containsKey( argument );
    }

    public String getUnprocessedInput() {
        return unprocessedInput;
    }

    @Override
    public String toString() {
        return "CommandInvocation{" +
                "arguments=" + arguments +
                ", unprocessedInput='" + unprocessedInput + '\'' +
                '}';
    }
}
