package com.athaydes.osgiaas.autocomplete.impl;

import com.athaydes.osgiaas.autocomplete.Autocompleter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A trivial implementation of {@link Autocompleter} that takes any option starting
 * with the provided text as a possible auto-completion.
 */
public class StartWithAutocompleter implements Autocompleter {

    @Override
    public List<String> completionsFor( String text, List<String> options ) {
        return options.stream()
                .filter( option -> option.startsWith( text ) )
                .collect( Collectors.toList() );
    }

}
