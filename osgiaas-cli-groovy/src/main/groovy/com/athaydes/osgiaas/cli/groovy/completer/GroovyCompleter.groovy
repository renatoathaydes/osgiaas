package com.athaydes.osgiaas.cli.groovy.completer

import com.athaydes.osgiaas.api.cli.CommandCompleter
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher
import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.cli.groovy.command.GroovyCommand
import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicReference

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
            return new DynamicCompleter( vars ).complete( buffer, cursor, candidates )
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
