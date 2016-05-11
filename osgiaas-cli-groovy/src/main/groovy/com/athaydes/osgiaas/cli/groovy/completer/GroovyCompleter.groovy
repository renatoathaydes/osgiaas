package com.athaydes.osgiaas.cli.groovy.completer

import com.athaydes.osgiaas.api.cli.CommandCompleter
import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher
import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.cli.groovy.command.GroovyCommand
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import javax.annotation.Nullable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicReference

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
                alternativeResult = new PropertiesCompleter( vars: vars )
                        .complete( buffer, cursor, candidates )
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

    private static final Map<Class, Class> boxedTypeByPrimitive = [
            ( boolean ): Boolean,
            ( byte )   : Byte,
            ( short )  : Short,
            ( char )   : Character,
            ( int )    : Integer,
            ( float )  : Float,
            ( double ) : Double,
            ( long )   : Long
    ].asImmutable()

    Map vars

    @Override
    int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        String input = buffer.substring( 0, cursor )

        if ( !input.contains( '.' ) ) {
            // no completion
            return -1
        }

        String finalPart = ''

        def breakupOptions = CommandHelper.CommandBreakupOptions.create()
                .includeQuotes( true )
                .quoteCodes( CommandHelper.DOUBLE_QUOTE_CODE, CommandHelper.SINGLE_QUOTE_CODE )

        // break up arguments
        CommandHelper.breakupArguments( input, { finalPart = it; true }, breakupOptions )

        // break up last argument into the actual tokens we're interested (method calls, property access)
        final tokens = [ ] as LinkedList<String>
        CommandHelper.breakupArguments( finalPart, {
            tokens << it; true
        }, breakupOptions.separatorCode( ( '.' as char ) as int ) )

        if ( finalPart.endsWith( '.' ) ) {
            tokens << ''
        }

        mergeDigitsIn tokens

        Class varType = Object
        boolean isClassInstance = false
        final toComplete = tokens.removeLast()
        final tokensIterator = tokens.iterator()

        if ( tokensIterator.hasNext() ) {
            final token = tokensIterator.next()

            // try to evaluate the first token with the existing bindings
            try {
                def result = new GroovyShell( new Binding( vars ) ).evaluate( token )

                if ( result != null ) {
                    if ( result instanceof Class ) {
                        varType = result
                        isClassInstance = true
                    } else {
                        varType = result.class
                    }
                }
            } catch ( ignore ) {
                varType = Object
            }
        }

        while ( tokensIterator.hasNext() ) {
            def token = tokensIterator.next()

            if ( token.contains( '(' ) && token.endsWith( ')' ) ) { // is method call?
                String methodName = token[ 0..<( token.indexOf( '(' ) ) ]
                def returnType = returnTypeOf( methodName, varType )

                if ( returnType ) {
                    varType = returnType
                    continue // got it
                }
            } else { // is property?
                def field = varType.fields.find { it.name == token }

                if ( field ) {
                    varType == field.type
                    continue // got it
                } else { // try getter
                    def methodName = 'get' + token.capitalize()
                    def returnType = returnTypeOf( methodName, varType )

                    if ( returnType ) {
                        varType = returnType
                        continue // got it
                    }
                }
            }

            // got here if nothing worked, varType unknown
            varType = Object
        }

        if ( varType ) {
            varType = boxedTypeByPrimitive[ varType ] ?: varType

            def allMethods = varType.methods.findAll(
                    isClassInstance ? this.&isStatic : this.&nonStatic
            ) as List<Method>

            if ( isClassInstance ) {
                allMethods.addAll( Class.getMethods() as List<Method> )
            } else {
                allMethods.addAll( DefaultGroovyMethods.methods.findAll {
                    it.parameterCount > 0 && it.parameterTypes.first().isAssignableFrom( varType )
                } )
            }

            candidates.addAll( allMethods.collectMany( this.&toCompletion ).findAll {
                ( it as String ).startsWith( toComplete )
            }.sort() )

            return input.findLastIndexOf { it == '.' } + 1
        } else {
            return -1
        }
    }

    private static List<String> toCompletion( Method method ) {
        if ( method.name.startsWith( 'get' ) &&
                method.name != 'get' &&
                method.parameterCount == 0 ) {
            return [ method.name + '()', uncapitalizeAscii( method.name - 'get' ) ]
        } else if ( method.parameterCount > 0 ) {
            return [ method.name + '(' ]
        } else {
            return [ method.name + '()' ]
        }
    }

    private static void mergeDigitsIn( LinkedList<String> tokens ) {
        if ( tokens.size() > 2 &&
                tokens[ -2 ].isDouble() &&
                tokens[ -3 ].isInteger() ) {
            String toComplete = tokens.removeLast()
            def last = tokens.removeLast()
            def penultimate = tokens.removeLast()
            tokens << "${penultimate}.${last}".toString() << toComplete
        }
    }

    private static uncapitalizeAscii( String word ) {
        char[] chars = word.toCharArray()
        chars[ 0 ] += 32
        new String( chars )
    }

    private static boolean isStatic( Method method ) {
        ( method.modifiers & Modifier.STATIC )
    }

    private static boolean nonStatic( Method method ) {
        !( method.modifiers & Modifier.STATIC )
    }

    @Nullable
    private static Class returnTypeOf( String method, Class type ) {
        def methods = type.methods.findAll { it.name == method }
        if ( methods ) {
            // simply choose the first matching method, later we can try to choose the right option
            return methods.first().returnType
        } else {
            return null
        }
    }

}
