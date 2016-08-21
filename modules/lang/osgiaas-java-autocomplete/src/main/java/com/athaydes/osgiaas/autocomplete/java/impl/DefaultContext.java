package com.athaydes.osgiaas.autocomplete.java.impl;

import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext;

import java.util.Collection;
import java.util.Collections;

public class DefaultContext implements JavaAutocompleteContext {

    private static final JavaAutocompleteContext INSTANCE = new DefaultContext();

    public static JavaAutocompleteContext getInstance() {
        return INSTANCE;
    }

    private DefaultContext() {
        // private
    }

    @Override
    public Collection<String> getImports() {
        return Collections.emptyList();
    }

    @Override
    public String getMethodBody( String codeSnippet ) {
        return codeSnippet;
    }
}
