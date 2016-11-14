package com.athaydes.osgiaas.autocomplete.java.impl;

import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class EmptyContext implements JavaAutocompleteContext {

    private static final JavaAutocompleteContext INSTANCE = new EmptyContext();

    public static JavaAutocompleteContext getInstance() {
        return INSTANCE;
    }

    private EmptyContext() {
        // private
    }

    @Override
    public Collection<String> getImports() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getVariables() {
        return Collections.emptyMap();
    }

    @Override
    public String getMethodBody( String codeSnippet ) {
        return codeSnippet;
    }
}
