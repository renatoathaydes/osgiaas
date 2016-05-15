package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.CommandInvocation;
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.api.service.DynamicServiceHelper;
import com.athaydes.osgiaas.cli.command.ListResourcesCommand;
import org.osgi.service.component.ComponentContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ListResourceCompleter implements CommandCompleter {

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();

    public void activate( ComponentContext context ) {
        contextRef.set( context );
    }

    public void deactivate( ComponentContext context ) {
        contextRef.set( null );
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        String input = buffer.substring( 0, cursor );
        if ( input.trim().startsWith( "lr" ) ) {
            return DynamicServiceHelper.let( contextRef, context -> {
                CommandInvocation invocation = ListResourcesCommand.argsSpec.parse( input );

                AtomicReference<String> toComplete = new AtomicReference<>();

                List<String> resources = ListResourcesCommand.listResources( invocation, context,
                        ListResourcesCommand.searchTransform.andThen( search -> {
                            String[] parts = splitSearchParts( search );
                            toComplete.set( parts[ 1 ] );
                            return parts[ 0 ];
                        } ) ).filter( suggestion -> {
                    String relevantSuggestionPart = relevantSuggestionPart( suggestion );
                    return relevantSuggestionPart.startsWith( toComplete.get() );
                } ).collect( Collectors.toList() );

                return new Completer( resources ).complete( buffer, cursor, candidates );
            }, () -> -1 );
        } else {
            return -1;
        }
    }

    // result: [0] = arg to listResources, [1] = value the options should startWith
    private static String[] splitSearchParts( String search ) {
        int lastSlashIndex = search.lastIndexOf( '/' );
        String[] result = new String[ 2 ];
        if ( lastSlashIndex >= 0 ) {
            result[ 0 ] = search.substring( 0, lastSlashIndex );
            result[ 1 ] = lastSlashIndex < search.length() ?
                    search.substring( lastSlashIndex + 1 ) : "";
        } else {
            result[ 0 ] = search;
            result[ 1 ] = "";
        }

        return result;
    }

    static String relevantSuggestionPart( String suggestion ) {
        int firstSlashIndex = suggestion.indexOf( '/' );

        if ( firstSlashIndex < 0 ) { // no slash
            return suggestion;
        }

        int lastSlashIndex = suggestion.lastIndexOf( '/' );

        if ( firstSlashIndex == lastSlashIndex && firstSlashIndex == suggestion.length() - 1 ) { // one slash at the end
            return suggestion;
        }

        if ( lastSlashIndex == suggestion.length() - 1 ) { // ends with a slash
            return splitSearchParts( suggestion.substring( 0, suggestion.length() - 1 ) )[ 1 ] + "/";
        }

        return splitSearchParts( suggestion )[ 1 ];
    }

    private static class Completer extends BaseCompleter {
        Completer( List<String> options ) {
            super( CompletionMatcher.nameMatcher( "lr", CompletionMatcher.alternativeMatchers( () ->
                    options.stream().map( CompletionMatcher::nameMatcher ) ) ) );
        }
    }

}
