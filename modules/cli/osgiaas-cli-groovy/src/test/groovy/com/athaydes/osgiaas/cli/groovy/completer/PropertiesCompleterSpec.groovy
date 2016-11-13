package com.athaydes.osgiaas.cli.groovy.completer

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class PropertiesCompleterSpec extends Specification {

    class Empty {}

    class A {
        String hello

        int getInteger() { 30 }
    }

    class B {
        void noReturn() {}

        boolean yesOrNo() { false }
    }

    def "Should complete arguments with simple properties"() {
        given: 'A couple of classes A and B with just  few methods'

        and: 'A Groovy runner mock that can see A and B'
        def groovyRunner = { String script ->
            new GroovyShell( new Binding( [ a: new A(), b: new B() ] ) ).evaluate( script )
        }

        and: 'A PropertiesCompleter with variables of types A and B available '
        def completer = new PropertiesCompleter( groovyRunner: groovyRunner )

        when: 'We request completions for arguments'
        def candidates = [ ]
        def returnValue = completer.complete( buffer, cursor, candidates )

        then: 'the expected completions are added'
        candidates as Set == expectedCompletions as Set

        and: 'the returns value is as expected'
        returnValue == expectedReturnValue

        where:
        buffer           | cursor | expectedCompletions | expectedReturnValue
        ''               | 0      | [ ]                 | -1
        'groovy a'       | 8      | [ ]                 | -1
        'groovy a.he'    | 11     | [ 'hello', ]        | 'groovy a.'.size()
        'groovy a.setH'  | 13     | [ 'setHello(', ]    | 'groovy a.'.size()
        'groovy a.int'   | 12     | [ 'integer', ]      | 'groovy a.'.size()
        'groovy b.noRe'  | 13     | [ 'noReturn()', ]   | 'groovy a.'.size()
        'groovy b.yesOr' | 14     | [ 'yesOrNo()', ]    | 'groovy a.'.size()
        'groovy x.xx'    | 11     | [ ]                 | -1
        'groovy zz.xx'   | 12     | [ ]                 | -1
        'groovy a.xx'    | 11     | [ ]                 | -1
        'groovy b.xx'    | 11     | [ ]                 | -1
    }

    def "Should complete arguments with properties which have many Groovy options"() {
        given: 'A couple of classes A and B with just  few methods'

        and: 'A Groovy runner mock that can see A and B'
        def groovyRunner = { String script ->
            new GroovyShell( new Binding( [ a: new A(), b: new B() ] ) ).evaluate( script )
        }

        and: 'A PropertiesCompleter with variables of types A and B available '
        def completer = new PropertiesCompleter( groovyRunner: groovyRunner )

        when: 'We request completions for arguments'
        def candidates = [ ]
        def returnValue = completer.complete( buffer, cursor, candidates )

        then: 'all expected completions contain at least one candidate in completions that starts with it'
        expectedCompletions.each { expectedCompletion ->
            // getters are converted to property
            if ( expectedCompletion.startsWith( 'get' ) ) {
                expectedCompletion = expectedCompletion.substring( 3 )
                expectedCompletion = expectedCompletion[ 0 ].toLowerCase() + expectedCompletion[ 1..-1 ]
            }
            assert expectedCompletion && candidates.any { it.startsWith( expectedCompletion ) }
        }

        and: 'the returns value is as expected'
        returnValue == expectedReturnValue

        where:
        buffer      | cursor | expectedCompletions                                       | expectedReturnValue
        'groovy a.' | 9      | [ 'hello', 'setHello(', 'integer' ] + Empty.methods*.name | 'groovy a.'.size()
        'groovy b.' | 9      | [ 'noReturn()', 'yesOrNo()' ] + Empty.methods*.name       | 'groovy a.'.size()
    }

}
