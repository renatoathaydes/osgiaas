package com.athaydes.osgiaas.cli.command

import com.athaydes.osgiaas.api.cli.CommandHelper
import spock.lang.Specification

class CommandHelperSpec extends Specification {

    def "CommandHelper can provide correct arguments from an invocation"() {
        when:
        def result = CommandHelper.parseCommandInvocation( command, maxArgs )

        then:
        result.argumentsAsMap == expectedResultMap

        and:
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command              | maxArgs | expectedResultMap                                   | expectedUnprocessedInput
        ''                   | 2       | [ : ]                                               | ''
        'hi'                 | 1       | [ 'hi': [ ] ]                                       | ''
        'hi'                 | 10      | [ 'hi': [ ] ]                                       | ''
        'hi -x'              | 2       | [ 'hi': [ ], '-x': [ ] ]                            | ''
        'hi -x -y a'         | 2       | [ 'hi': [ ], '-x': [ ] ]                            | '-y a'
        'hi -x -y a'         | 200     | [ 'hi': [ ], '-x': [ ], '-y': [ 'a' ] ]             | ''
        'hi -y a -y b'       | 200     | [ 'hi': [ ], '-y': [ 'a', 'b' ] ]                   | ''
        'x y z'              | 4       | [ x: [ ], y: [ ], z: [ ] ]                          | ''
        'x y z'              | 3       | [ x: [ ], y: [ ], z: [ ] ]                          | ''
        'x y z'              | 2       | [ x: [ ], y: [ ] ]                                  | 'z'
        'x y z'              | 1       | [ x: [ ] ]                                          | 'y z'
        'x y z'              | 0       | [ : ]                                               | 'x y z'
        'x -y z -z y w'      | 200     | [ x: [ ], '-y': [ 'z' ], '-z': [ 'y' ], w: [ ] ]    | ''
        'x -y z -z y -w'     | 200     | [ x: [ ], '-y': [ 'z' ], '-z': [ 'y' ], '-w': [ ] ] | ''
        'hl -F 0 ab cd'      | 5       | [ hl: [ ], '-F': [ '0' ], ab: [ ], cd: [ ] ]        | ''
        'hl -F 0 -B 3 ab cd' | 5       | [ hl: [ ], '-F': [ '0' ], '-B': [ '3' ] ]           | 'ab cd'
        'hl -F 0 -B 3 -F 1'  | 5       | [ hl: [ ], '-F': [ '0' ], '-B': [ '3' ] ]           | '-F 1'
    }

}
