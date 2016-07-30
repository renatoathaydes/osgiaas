package com.athaydes.osgiaas.autocomplete.impl;

import com.athaydes.osgiaas.autocomplete.Autocompleter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsgiaasAutocompleter implements Autocompleter {

    private final Pattern uppercasePattern = Pattern.compile( "(?=[A-Z])" );

    @Override
    public List<String> completionsFor( String text, List<String> options ) {
        Iterator<String> words = breakupWords( text ).iterator();
        List<Completion> completions = options.stream()
                .map( option -> new Completion( option, breakupWords( option ).iterator() ) )
                .collect( Collectors.toList() );

        while ( words.hasNext() ) {
            String word = words.next();
            completions = completions.stream().map( completion -> {
                if ( completion.words.hasNext() ) {
                    String optionWord = completion.words.next();
                    if ( optionWord.startsWith( word ) ) {
                        return completion;
                    }
                }
                return null;
            } ).filter( Objects::nonNull ).collect( Collectors.toList() );

            if ( completions.isEmpty() ) {
                return Collections.emptyList();
            }
        }

        return completions.stream()
                .map( it -> it.fullText )
                .collect( Collectors.toList() );
    }

    private Stream<String> breakupWords( String text ) {
        return uppercasePattern.splitAsStream( text );
    }

    private static class Completion {
        final String fullText;
        final Iterator<String> words;

        Completion( String fullText, Iterator<String> words ) {
            this.fullText = fullText;
            this.words = words;
        }
    }

}

