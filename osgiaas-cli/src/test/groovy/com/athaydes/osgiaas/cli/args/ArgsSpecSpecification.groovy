package com.athaydes.osgiaas.cli.args

import com.athaydes.osgiaas.api.cli.args.ArgsSpec
import spock.lang.Specification

class ArgsSpecSpecification extends Specification {

    def "It should be very easy to specify a command arguments"() {
        given: 'An ArgsSpec is built with a few simple options'
        def spec = ArgsSpec.builder()
        //            option, mandatory?, takes a parameter?
                .accepts( '-f', true, true )
                .accepts( '--no-fuss' )
                .noFurtherArgumentsAllowed()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.argumentsAsMap == expectedResult

        where:
        command              | expectedResult
        'x -f a'             | [ '-f': [ 'a' ] ]
        'x -f abc --no-fuss' | [ '-f': [ 'abc' ], '--no-fuss': [ ] ]
    }

}
