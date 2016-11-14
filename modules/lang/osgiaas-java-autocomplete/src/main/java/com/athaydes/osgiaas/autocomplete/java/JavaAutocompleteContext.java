package com.athaydes.osgiaas.autocomplete.java;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.autocomplete.java.impl.EmptyContext;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * A context for Java autocompletion.
 */
public interface JavaAutocompleteContext {

    /**
     * @return optional classLoader context to use when searching for class names and class definitions.
     * If not provided, the classLoader the loaded the implementation of this class will be used where possible.
     */
    default Optional<ClassLoaderContext> getClassLoaderContext() {
        return Optional.empty();
    }

    Collection<String> getImports();

    Map<String, Object> getVariables();

    String getMethodBody( String codeSnippet );

    static JavaAutocompleteContext emptyContext() {
        return EmptyContext.getInstance();
    }

}
