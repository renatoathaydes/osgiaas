package com.athaydes.osgiaas.cli.groovy.completer

import com.athaydes.osgiaas.api.cli.CommandCompleter
import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher
import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.api.stream.NoOpPrintStream
import com.athaydes.osgiaas.cli.groovy.command.GroovyCommand
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import javax.annotation.Nullable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.nameMatcher

@CompileStatic
class GroovyCompleter implements CommandCompleter {

    final AtomicReference<GroovyCommand> groovyRef = new AtomicReference<>()

    @Nullable
    KnowsCommandBeingUsed knowsCommandBeingUsed = null

    final BaseCompleter argsMatcher = new BaseCompleter( nameMatcher( 'groovy',
            CompletionMatcher.alternativeMatchers(
                    nameMatcher( GroovyCommand.SHOW_PRE_ARG ),
                    nameMatcher( GroovyCommand.CLEAN_PRE_ARG ),
                    nameMatcher( GroovyCommand.ADD_PRE_ARG )
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

        DynamicServiceHelper.let( groovyRef, { GroovyCommand groovy ->
            int alternativeResult = new DynamicCompleter(
                    groovy.shell.context.variables, knowsCommandBeingUsed )
                    .complete( buffer, cursor, candidates )

            if ( alternativeResult < 0 ) {
                alternativeResult = new PropertiesCompleter( groovyRunner: { String script ->
                    def noopStream = new NoOpPrintStream()
                    groovy.run( script, noopStream, noopStream )
                } ).complete( buffer, cursor, candidates )
            }

            if ( alternativeResult >= 0 ) {
                return alternativeResult
            } else {
                return result
            }
        }, { result } )
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

    Function<String, Object> groovyRunner

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
            tokens << it.replaceAll( ' ', '' ); true
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
                def result = groovyRunner.apply( token )

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
                def field = varType.fields.find { it.name == token } as Field

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

            if ( !isClassInstance ) {
                isClassInstance = ( varType == Class )
            }

            def methods = varType.methods.findAll(
                    // for class instances, include only static methods, for others, include all
                    isClassInstance ? this.&isStatic : { true }
            ) as List<Method>

            List<Method> extraMethods

            if ( isClassInstance ) {
                extraMethods = Class.getMethods() as List<Method>
            } else {
                extraMethods = DefaultGroovyMethods.methods.findAll {
                    it.parameterCount > 0 && it.parameterTypes.first().isAssignableFrom( varType )
                } as List<Method>
            }

            candidates.addAll( ( methods.sort { it.name } + extraMethods.sort { it.name } )
                    .collectMany( this.&toCompletion ).findAll {
                ( it as String ).startsWith( toComplete )
            }.unique() )

            if ( candidates ) {
                return input.findLastIndexOf { it == '.' } + 1
            }
        }

        return -1
    }

    private static List<String> toCompletion( Method method ) {
        // for DefaultGroovyMethods, the first parameter is "self" and should not be counted
        def compensatingFactor = method.declaringClass == DefaultGroovyMethods ? -1 : 0
        def parameterCount = method.parameterCount + compensatingFactor

        if ( method.name.startsWith( 'get' ) &&
                method.name != 'get' &&
                parameterCount == 0 ) {
            return [ method.name + '()', uncapitalizeAscii( method.name - 'get' ) ]
        } else if ( parameterCount > 0 ) {
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
