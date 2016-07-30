package com.athaydes.osgiaas.autocomplete;

import com.athaydes.osgiaas.autocomplete.impl.OsgiaasAutocompleter;

import java.util.List;

/**
 * API for the OSGiaaS auto-completion library.
 */
public interface Autocompleter {

    /**
     * Given some text and available options, returns all possible completions for the given text.
     *
     * @param text    to autocomplete
     * @param options for autocompletion
     * @return possible completions
     */
    List<String> completionsFor( String text, List<String> options );

    /**
     * @return the default {@link Autocompleter} implementation.
     * The default behaviour is to use capital letters as word boundaries
     * so that text like `gA` can be auto-completed with `getAll`, for example.
     */
    static Autocompleter getDefaultAutocomplete() {
        return new OsgiaasAutocompleter();
    }

}
