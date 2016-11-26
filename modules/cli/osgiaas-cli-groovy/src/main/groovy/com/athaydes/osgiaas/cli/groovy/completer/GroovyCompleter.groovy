package com.athaydes.osgiaas.cli.groovy.completer

import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.api.stream.NoOpPrintStream
import com.athaydes.osgiaas.autocomplete.Autocompleter
import com.athaydes.osgiaas.cli.CommandCompleter
import com.athaydes.osgiaas.cli.CommandHelper
import com.athaydes.osgiaas.cli.completer.BaseCompleter
import com.athaydes.osgiaas.cli.completer.CompletionMatcher
import com.athaydes.osgiaas.cli.groovy.command.GroovyCommand
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import javax.annotation.Nullable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

import static CompletionMatcher.nameMatcher

//@CompileStatic
class GroovyCompleter implements CommandCompleter {

    final AtomicReference<GroovyCommand> groovyRef = new AtomicReference<>()

    final BaseCompleter argsMatcher = new BaseCompleter( nameMatcher( 'groovy',
            CompletionMatcher.alternativeMatchers(
                    nameMatcher( GroovyCommand.RESET_CODE_ARG ),
                    nameMatcher( GroovyCommand.SHOW_ARG ),
                    nameMatcher( 'System' ),
                    nameMatcher( 'def' )
            ) ) )

    @Override
    int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        if ( !buffer.startsWith( 'groovy ' ) ) {
            // only auto-complete groovy
            return -1
        }

        int result = argsMatcher.complete( buffer, cursor, candidates )

        DynamicServiceHelper.let( groovyRef, { GroovyCommand groovy ->
            int alternativeResult = new DynamicCompleter( groovy.shell.context.variables )
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

    DynamicCompleter( Map vars ) {
        super( nameMatcher( 'groovy',
                vars.keySet().collect {
                    nameMatcher( it as String )
                } as CompletionMatcher[] ) )
    }
}

@CompileStatic
class PropertiesCompleter implements CommandCompleter {

    private static final Map<Class, ResultType> boxedTypeByPrimitive = [
            ( boolean ): Boolean,
            ( byte )   : Byte,
            ( short )  : Short,
            ( char )   : Character,
            ( int )    : Integer,
            ( float )  : Float,
            ( double ) : Double,
            ( long )   : Long
    ].collectEntries { k, v -> [ ( k ): ResultType.create( v ) ] }.asImmutable() as Map<Class, ResultType>

    final Autocompleter autocompleter = Autocompleter.getDefaultAutocompleter()

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

        ResultType varType = ResultType.VOID
        final toComplete = tokens.removeLast()
        final tokensIterator = tokens.iterator()

        if ( tokensIterator.hasNext() ) {
            final token = tokensIterator.next()

            // try to evaluate the first token with the existing bindings
            try {
                def result = groovyRunner.apply( token )

                if ( result != null ) {
                    if ( result instanceof Class ) {
                        varType = new ResultType( result, true )
                    } else {
                        varType = new ResultType( result.class, false )
                    }
                }
            } catch ( ignore ) {
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
                def field = varType.type.fields.find { it.name == token } as Field

                if ( field ) {
                    varType = ResultType.create( field.type )
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
            varType = ResultType.VOID
        }

        if ( varType ) {
            varType = boxedTypeByPrimitive[ varType.type ] ?: varType

            def fields = varType.type.fields
                    .collect { Field f -> f.name }
                    .sort() as List<String>

            // for class instances, include only static methods, for others, include all
            def methodFilter = varType.isStatic ? this.&isStatic : { true }
            def methods = varType.type.methods.findAll( methodFilter ) as List<Method>

            List<Method> extraMethods

            if ( varType.isStatic ) {
                extraMethods = Class.getMethods() as List<Method>
            } else {
                extraMethods = DefaultGroovyMethods.methods.findAll {
                    it.parameterCount > 0 && it.parameterTypes.first().isAssignableFrom( varType.type )
                } as List<Method>
            }

            def completions = autocompleter.completionsFor( toComplete,
                    ( ( methods.sort { it.name } + extraMethods.sort { it.name } )
                            .collectMany( this.&toCompletion ) + fields ).unique() )

            if ( completions ) {
                candidates.addAll( completions )
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
            return [ uncapitalizeAscii( method.name - 'get' ) ]
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
    private static ResultType returnTypeOf( String method, ResultType resultType ) {
        def type = resultType.type
        def keepMethod = { Method m -> resultType.isStatic ? isStatic( m ) : !isStatic( m ) }

        def methods = type.methods.findAll { keepMethod( it ) && it.name == method }

        if ( methods ) {
            // simply choose the first matching method, later we can try to choose the right option
            def methodType = methods.first().returnType
            def isStatic = methodType == Class
            return new ResultType( methodType, isStatic )
        } else {
            return null
        }
    }

}

@Immutable
class ResultType {
    static final ResultType VOID = new ResultType( Void.TYPE, false )
    final Class<?> type
    final boolean isStatic

    static ResultType create( Class<?> type ) {
        new ResultType( type, type == Class )
    }
}