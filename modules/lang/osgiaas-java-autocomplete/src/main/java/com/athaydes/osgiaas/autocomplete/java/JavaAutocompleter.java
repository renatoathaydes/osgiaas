package com.athaydes.osgiaas.autocomplete.java;

import com.athaydes.osgiaas.autocomplete.Autocompleter;
import com.athaydes.osgiaas.autocomplete.java.impl.EmptyContext;
import com.athaydes.osgiaas.autocomplete.java.impl.OsgiaasJavaAutocompleter;

import java.util.Map;

/**
 * API for the OSGiaaS Java language auto-completer.
 */
public interface JavaAutocompleter {

    /**
     * Given some text and available options, returns all possible completions for the given text.
     *
     * @param codeFragment to autocomplete
     * @param bindings     current bindings the Java code has access to
     * @return possible completions
     */
    JavaAutocompleterResult completionsFor( String codeFragment, Map<String, Object> bindings );

    /**
     * @return the default {@link JavaAutocompleter} implementation, which uses a
     * default {@link Autocompleter} and an empty context.
     */
    static JavaAutocompleter getDefaultAutocompleter() {
        return new OsgiaasJavaAutocompleter(
                Autocompleter.getDefaultAutocompleter(),
                EmptyContext.getInstance() );
    }

    /**
     * @return the default {@link JavaAutocompleter} implementation,
     * based on the provided autocompleter and an empty context.
     */
    static JavaAutocompleter getAutocompleter( Autocompleter autocompleter ) {
        return new OsgiaasJavaAutocompleter( autocompleter, EmptyContext.getInstance() );
    }

    /**
     * @return the default {@link JavaAutocompleter} implementation,
     * based on the provided autocompleter and context.
     */
    static JavaAutocompleter getAutocompleter( Autocompleter autocompleter,
                                               JavaAutocompleteContext context ) {
        return new OsgiaasJavaAutocompleter( autocompleter, context );
    }

}
