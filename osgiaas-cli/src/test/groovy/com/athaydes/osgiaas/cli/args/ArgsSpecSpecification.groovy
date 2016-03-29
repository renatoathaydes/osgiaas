package com.athaydes.osgiaas.cli.args

import com.athaydes.osgiaas.api.cli.args.ArgsSpec
import spock.lang.Specification
import spock.lang.Unroll

class ArgsSpecSpecification extends Specification {

    @Unroll
    def "It should be very easy to specify a command arguments"() {
        given: 'An ArgsSpec is built with a few simple options'
        def spec = ArgsSpec.builder()
        //            option, mandatory?, takes a parameter?
                .accepts( '-f', true, true )
                .accepts( '--no-fuss' )
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.arguments == expectedArgs
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command                      | expectedArgs                          | expectedUnprocessedInput
        'x -f a'                     | [ '-f': [ 'a' ] ]                     | ''
        'x -f abc --no-fuss'         | [ '-f': [ 'abc' ], '--no-fuss': [ ] ] | ''
        'x -f abc something'         | [ '-f': [ 'abc' ] ]                   | 'something'
        'x --no-fuss -f a and stuff' | [ '-f': [ 'a' ], '--no-fuss': [ ] ]   | 'and stuff'
    }

    def "Missing mandatory argument should cause an error"() {
        given: 'An ArgsSpec is built with a few options, including mandatory options'
        def spec = ArgsSpec.builder()
        //            option, mandatory?, takes a parameter?
                .accepts( '-f', true, true )
                .accepts( 'a', true, false )
                .accepts( '--no-fuss' )
                .build()

        when: 'some example command invocations are parsed'
        spec.parse( command )

        then: 'an error is thrown with an appropriate message'
        def error = thrown( IllegalArgumentException )
        !error.message?.empty
        expectedErrors.every { error.message.contains( it ) }

        where:
        command              | expectedErrors
        'cmd'                | [ 'a', '-f' ]
        'cmd a'              | [ '-f' ]
        'cmd --no-fuss'      | [ 'a', '-f' ]
        'cmd -f a'           | [ 'a' ]
        'cmd -f b --no-fuss' | [ 'a' ]
        'cmd -f b c'         | [ 'a' ]
    }

    def "Missing argument to parameter causes an error"() {
        given: 'An ArgsSpec is built with a few options, including mandatory argument options'
        def spec = ArgsSpec.builder()
        //            option, mandatory?, takes a parameter?
                .accepts( '-f', true, true )
                .accepts( 'a', false, true )
                .accepts( '--no-fuss' )
                .build()

        when: 'some example command invocations are parsed'
        spec.parse( command )

        then: 'an error is thrown with an appropriate message'
        def error = thrown( IllegalArgumentException )
        !error.message?.empty
        error.message.contains( expectedError )

        where:
        command                | expectedError
        'cmd -f'               | '-f'
        'cmd -f 1 a'           | 'a'
        'cmd -f 1 --no-fuss a' | 'a'
    }

}
