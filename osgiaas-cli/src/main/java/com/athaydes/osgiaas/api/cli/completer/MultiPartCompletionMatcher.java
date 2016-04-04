package com.athaydes.osgiaas.api.cli.completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class MultiPartCompletionMatcher extends ParentCompletionMatcher {

    private final String separator;
    private final List<CompletionMatcherCollection> completionMatchers;

    MultiPartCompletionMatcher( String separator,
                                List<CompletionMatcherCollection> completionMatchers,
                                List<CompletionMatcher> children ) {
        super( children );
        this.separator = separator;
        this.completionMatchers = completionMatchers;
    }

    @Override
    public List<String> completionsFor( String argument ) {
        List<String> partsSink = new ArrayList<>( completionMatchers.size() );
        LinkedList<String> consumableParts = splitParts( argument );
        Iterator<CompletionMatcherCollection> matchers = completionMatchers.iterator();

        String part = null;
        CompletionMatcher matcher = null;
        boolean useCurrentMatcher = false;

        // walk through parts and matchers, aborting in case a matcher does not fully match
        while ( !consumableParts.isEmpty() && matchers.hasNext() ) {
            part = consumableParts.removeFirst();
            matcher = matchers.next();

            if ( !matcher.argumentFullyMatched( part ) ) {
                useCurrentMatcher = true;
                break;
            }

            partsSink.add( part );
        }

        // complete if the current matcher is to be used and this is the last part
        if ( part != null && useCurrentMatcher && consumableParts.isEmpty() ) {

            String prefix = partsSink.isEmpty() ?
                    "" :
                    String.join( separator, partsSink ) + separator;

            return matcher.completionsFor( part ).stream()
                    .map( completion -> prefix + completion )
                    .collect( Collectors.toList() );

            // complete if there's no more parts but there are more matchers and the previous matcher matched fully
        } else if ( consumableParts.isEmpty() &&
                matcher != null &&
                matcher.argumentFullyMatched( part ) &&
                matchers.hasNext() ) {

            String prefix = partsSink.isEmpty() ?
                    separator :
                    String.join( separator, partsSink ) + separator;

            return matchers.next().completionsFor( "" ).stream()
                    .map( completion -> prefix + completion )
                    .collect( Collectors.toList() );
        } else {
            return Collections.emptyList();
        }
    }

    private LinkedList<String> splitParts( String argument ) {
        LinkedList<String> result = new LinkedList<>( Arrays.asList( argument.split( separator ) ) );
        if ( argument.endsWith( separator ) ) {
            // trailing separator should trigger completion
            result.add( "" );
        }
        return result;
    }

    @Override
    public boolean partiallyMatches( String command ) {
        return false;
    }
}
