package com.athaydes.osgiaas.api.cli;

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

    public String getUnprocessedInput() {
        return unprocessedInput;
    }

}
