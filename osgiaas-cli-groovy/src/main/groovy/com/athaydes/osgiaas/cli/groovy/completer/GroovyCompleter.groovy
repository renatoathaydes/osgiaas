package com.athaydes.osgiaas.cli.groovy.completer

import com.athaydes.osgiaas.api.cli.CommandCompleter
import com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher
import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.cli.groovy.command.GroovyCommand
import groovy.transform.CompileStatic

import javax.annotation.Nullable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.nameMatcher
import static com.athaydes.osgiaas.cli.groovy.command.GroovyCommand.ADD_PRE_ARG
import static com.athaydes.osgiaas.cli.groovy.command.GroovyCommand.CLEAN_PRE_ARG
import static com.athaydes.osgiaas.cli.groovy.command.GroovyCommand.SHOW_PRE_ARG

@CompileStatic
class GroovyCompleter implements CommandCompleter {

    final AtomicReference<GroovyCommand> groovyRef = new AtomicReference<>()

    @Nullable
    KnowsCommandBeingUsed knowsCommandBeingUsed = null

    final BaseCompleter argsMatcher = new BaseCompleter( nameMatcher( 'groovy',
            CompletionMatcher.alternativeMatchers(
                    nameMatcher( SHOW_PRE_ARG ),
                    nameMatcher( CLEAN_PRE_ARG ),
                    nameMatcher( ADD_PRE_ARG )
            ) ) )

    @Override
    int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        String prefix = buffer
        KnowsCommandBeingUsed knowsCommandBeingUsed = this.knowsCommandBeingUsed
        if ( knowsCommandBeingUsed?.using() ) {
            prefix = knowsCommandBeingUsed.using() + " $prefix"
        }

        if ( !prefix.startsWith( 'groovy ' ) ) {
            // only auto-complete groovy
            return -1
        }

        int result = argsMatcher.complete( buffer, cursor, candidates )

        Map vars = DynamicServiceHelper.let( groovyRef, { GroovyCommand groovy ->
            groovy.shell.context.variables
        }, { [ : ] } )

        if ( vars ) {
            int alternativeResult = new DynamicCompleter( vars, knowsCommandBeingUsed )
                    .complete( buffer, cursor, candidates )
            if ( alternativeResult < 0 ) {
                alternativeResult = new PropertiesCompleter( vars: vars ).complete( buffer, cursor, candidates )
            }

            if ( alternativeResult >= 0 ) {
                result = alternativeResult
            }
        }

        return result
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

    DynamicCompleter( Map vars, KnowsCommandBeingUsed knowsCommandBeingUsed ) {
        super( nameMatcher( 'groovy',
                vars.keySet().collect {
                    nameMatcher( it as String )
                } as CompletionMatcher[] ),
                knowsCommandBeingUsed )
    }
}

@CompileStatic
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
            String firstPart = parts[ 0 ].trim()

            def var = vars[ firstPart ]

            if ( var == null ) {
                if ( firstPart.isInteger() ) {
                    var = 0
                } else if ( firstPart.isFloat() ) {
                    var = 0f
                } else if ( firstPart.isLong() ) {
                    var = 0L
                } else if ( firstPart.isDouble() ) {
                    var = 0D
                } else if ( firstPart.matches( /['"].*['"]/ ) ) {
                    var = ''
                }
            }

            if ( var != null ) {
                boolean typeVar = var instanceof Class

                def filter = typeVar ?
                        this.&isStatic :
                        this.&nonStatic

                if ( !typeVar ) {
                    var = var.class
                }

                candidates.addAll( ( var as Class ).methods.findAll {
                    filter( it ) && it.name.startsWith( parts[ 1 ] )
                }*.name )

                return lastDotIndex + 1
            }
        }

        return -1
    }

    private static boolean isStatic( Method method ) {
        ( method.modifiers & Modifier.STATIC )
    }

    private static boolean nonStatic( Method method ) {
        !( method.modifiers & Modifier.STATIC )
    }

}
