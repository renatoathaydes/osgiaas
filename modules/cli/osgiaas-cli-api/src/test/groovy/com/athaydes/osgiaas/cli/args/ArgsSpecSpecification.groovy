package com.athaydes.osgiaas.cli.args

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ArgsSpecSpecification extends Specification {

    def "It should be very easy to specify command arguments"() {
        given: 'An ArgsSpec is built with a few simple options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).mandatory().withArgCount( 1 ).end()
                .accepts( '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.options == expectedArgs
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command                      | expectedArgs                                  | expectedUnprocessedInput
        'x -f a'                     | [ '-f': [ [ 'a' ] ] ]                         | ''
        'x -f abc --no-fuss'         | [ '-f': [ [ 'abc' ] ], '--no-fuss': [ [ ] ] ] | ''
        'x -f abc something'         | [ '-f': [ [ 'abc' ] ] ]                       | 'something'
        'x --no-fuss -f a and stuff' | [ '-f': [ [ 'a' ] ], '--no-fuss': [ [ ] ] ]   | 'and stuff'
    }

    def "It should be easy to specify long form command arguments"() {
        given: 'An ArgsSpec is built with a few simple options and long options'
        def spec = ArgsSpec.builder()
                .accepts( '-f', '--fuss' ).mandatory().withArgCount( 1 ).end()
                .accepts( '-n', '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.options == expectedArgs
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command                          | expectedArgs                           | expectedUnprocessedInput
        'x -f a'                         | [ '-f': [ [ 'a' ] ] ]                  | ''
        'x --fuss a'                     | [ '-f': [ [ 'a' ] ] ]                  | ''
        'x -f abc --no-fuss'             | [ '-f': [ [ 'abc' ] ], '-n': [ [ ] ] ] | ''
        'x -f abc -n'                    | [ '-f': [ [ 'abc' ] ], '-n': [ [ ] ] ] | ''
        'x -f abc something'             | [ '-f': [ [ 'abc' ] ] ]                | 'something'
        'x --fuss abc something'         | [ '-f': [ [ 'abc' ] ] ]                | 'something'
        'x --no-fuss --fuss a and stuff' | [ '-f': [ [ 'a' ] ], '-n': [ [ ] ] ]   | 'and stuff'
    }

    def "Command options may take multiple arguments"() {
        given: 'An ArgsSpec is built with options that take multiple options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).allowMultiple().withArgCount( 1, 3 ).end()
                .accepts( '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.options == expectedArgs
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command                               | expectedArgs                                                | expectedUnprocessedInput
        'x -f a -f b'                         | [ '-f': [ [ 'a' ], [ 'b' ] ] ]                              | ''
        'x -f abc --no-fuss -f d'             | [ '-f': [ [ 'abc' ], [ 'd' ] ], '--no-fuss': [ [ ] ] ]      | ''
        'x -f abc something'                  | [ '-f': [ [ 'abc', 'something' ] ] ]                        | ''
        'x  -f a and stuff'                   | [ '-f': [ [ 'a', 'and', 'stuff' ] ] ]                       | ''
        'x  -f a and stuff b'                 | [ '-f': [ [ 'a', 'and', 'stuff' ] ] ]                       | 'b'
        'x  -f a and stuff --no-fuss zz -f x' | [ '-f': [ [ 'a', 'and', 'stuff' ] ], '--no-fuss': [ [ ] ] ] | 'zz -f x'
        'x -f a b --no-fuss c'                | [ '-f': [ [ 'a', 'b' ] ], '--no-fuss': [ [ ] ] ]            | 'c'
        'x -f a b -f c d e f'                 | [ '-f': [ [ 'a', 'b' ], [ 'c', 'd', 'e' ] ] ]               | 'f'
    }

    def "Missing mandatory argument should cause an error"() {
        given: 'An ArgsSpec is built with a few options, including mandatory options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).mandatory().withArgCount( 1, 3 ).end()
                .accepts( 'a' ).mandatory().end()
                .accepts( '-n', '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        spec.parse( command )

        then: 'an error is thrown with an appropriate message'
        def error = thrown( IllegalArgumentException )
        !error.message?.empty
        error && expectedErrors.every { error.message.contains( it ) }

        where:
        command              | expectedErrors
        'cmd'                | [ 'a', '-f' ]
        'cmd a'              | [ '-f' ]
        'cmd --no-fuss'      | [ 'a', '-f' ]
        'cmd -n'             | [ 'a', '-f' ]
        'cmd -f a'           | [ 'a' ]
        'cmd -f b --no-fuss' | [ 'a' ]
        'cmd -f b -n'        | [ 'a' ]
        'cmd -f b c'         | [ 'a' ]
    }

    def "Missing argument to parameter causes an error"() {
        given: 'An ArgsSpec is built with a few options, including mandatory argument options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).mandatory().withArgCount( 1, 3 ).end()
                .accepts( '-a', '--aaa' ).withArgCount( 1 ).end()
                .accepts( '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        spec.parse( command )

        then: 'an error is thrown with an appropriate message'
        def error = thrown( IllegalArgumentException )
        !error.message?.empty
        error.message.contains( expectedError )

        where:
        command                 | expectedError
        'cmd -f'                | '-f'
        'cmd -f 1 -a'           | '-a'
        'cmd -f 1 --aaa'        | '-a'
        'cmd -f 1 --no-fuss -a' | '-a'
    }

    def "Command itself must always be removed from the results"() {
        given: 'An ArgsSpec with no mandatory options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).withArgCount( 1 ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the results never include the command itself'
        result.unprocessedInput == expectedUnprocessedInput
        result.options == expectedArgs

        where:
        command            | expectedArgs          | expectedUnprocessedInput
        'hello'            | [ : ]                 | ''
        'hello hi'         | [ : ]                 | 'hi'
        'hello -f a'       | [ '-f': [ [ 'a' ] ] ] | ''
        'hello -f a b'     | [ '-f': [ [ 'a' ] ] ] | 'b'
        'hello -f a b cde' | [ '-f': [ [ 'a' ] ] ] | 'b cde'
    }

    def "Using negative argument count range should cause an error"() {
        when: 'An ArgsSpec is created with argument containing a negative range'
        ArgsSpec.builder()
                .accepts( 'a' ).withArgCount( start, end ).end()
                .build()

        then: 'An error is thrown'
        thrown IllegalArgumentException

        where:
        start | end
        1     | -1
        -1    | 1
        -10   | -30
    }

}
