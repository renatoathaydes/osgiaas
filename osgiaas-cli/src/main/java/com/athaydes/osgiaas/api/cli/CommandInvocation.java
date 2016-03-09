package com.athaydes.osgiaas.api.cli;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of command invocation.
 */
public class CommandInvocation {

    private final List<CommandArgument> arguments;
    private final String unprocessedInput;

    public CommandInvocation( List<CommandArgument> arguments,
                              String unprocessedInput ) {
        this.arguments = arguments;
        this.unprocessedInput = unprocessedInput;
    }

    public List<CommandArgument> getArguments() {
        return arguments;
    }

    public Map<String, List<String>> getArgumentsAsMap() {
        Map<String, List<String>> result = new HashMap<>( arguments.size() );
        arguments.forEach( arg -> result.put( arg.getArgumentKey(), arg.getArgumentValues() ) );
        return result;
    }

    public String getUnprocessedInput() {
        return unprocessedInput;
    }

}
