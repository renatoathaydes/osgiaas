package com.athaydes.osgiaas.api.cli;

import java.util.List;

/**
 * Definition of an argument a command can take.
 */
public class CommandArgument {

    private final String argumentKey;
    private final List<String> argumentValues;

    CommandArgument( String argumentKey, List<String> argumentValues ) {
        this.argumentKey = argumentKey;
        this.argumentValues = argumentValues;
    }

    /**
     * @return if at least one value was provided for this argument.
     */
    public boolean isValueProvided() {
        return !argumentValues.isEmpty();
    }

    public String getArgumentKey() {
        return argumentKey;
    }

    public List<String> getArgumentValues() {
        return argumentValues;
    }

}
