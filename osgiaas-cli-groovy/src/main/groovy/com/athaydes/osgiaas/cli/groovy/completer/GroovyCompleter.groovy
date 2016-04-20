package com.athaydes.osgiaas.cli.groovy.completer

import com.athaydes.osgiaas.api.cli.CommandCompleter
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher
import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.cli.groovy.command.GroovyCommand
import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

@CompileStatic
class GroovyCompleter implements CommandCompleter {

    final AtomicReference<GroovyCommand> groovyRef = new AtomicReference<>()

    @Override
    int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        def variablesRef = new AtomicReference<Map>()
        DynamicServiceHelper.with( groovyRef ) { GroovyCommand groovy ->
            Map vars = groovy.shell.context.variables
            variablesRef.set vars
        }

        Map vars = variablesRef.get()

        if ( vars ) {
            int result = new DynamicCompleter( vars ).complete( buffer, cursor, candidates )
            if ( result < 0 ) {
                result = new PropertiesCompleter( vars: vars ).complete( buffer, cursor, candidates )
            }
            return result
        } else {
            return -1
        }
    }

    void setGroovyCommand( GroovyCommand command ) {
        groovyRef.set command
    }

    void unsetGroovyCommand( GroovyCommand command ) {
        groovyRef.set null
    }
}

@CompileStatic
class DynamicCompleter extends BaseCompleter {

    DynamicCompleter( Map vars ) {
        super( CompletionMatcher.nameMatcher( 'groovy',
                vars.keySet().collect {
                    CompletionMatcher.nameMatcher( it as String )
                } ) )
    }
}

class PropertiesCompleter implements CommandCompleter {

    Map vars

    @Override
    int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        String input = buffer.substring( 0, cursor )

        int lastDotIndex = input.lastIndexOf( '.' )

        int lastSpaceIndex = input.lastIndexOf( ' ' )
        if ( lastSpaceIndex > 0 ) {
            input = input.substring( Math.min( lastSpaceIndex + 1, input.size() ) )
        }

        String[] parts = input.split( Pattern.quote( '.' ) )
        if ( input.endsWith( '.' ) ) {
            parts += ''
        }

        if ( parts.size() == 2 ) {
            Class varType = vars[ parts[ 0 ] ]?.class
            if ( varType ) {
                candidates.addAll( varType.methods.findAll { it.name.startsWith( parts[ 1 ] ) }*.name )
                return lastDotIndex + 1
            }
        }

        return -1
    }

}
