package com.athaydes.osgiaas.autocomplete.java;

import com.athaydes.osgiaas.autocomplete.java.impl.DefaultContext;

import java.util.List;

/**
 * A context for Java autocompletion.
 */
public interface JavaAutocompleteContext {

    List<String> getImports();

    String getMethodBody( String codeSnippet );

    static JavaAutocompleteContext emptyContext() {
        return DefaultContext.getInstance();
    }

}
