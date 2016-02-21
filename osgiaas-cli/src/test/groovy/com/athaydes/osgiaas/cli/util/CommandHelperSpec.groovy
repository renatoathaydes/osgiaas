package com.athaydes.osgiaas.cli.util

import spock.lang.Specification
import spock.lang.Unroll

class CommandHelperSpec extends Specification {

    @Unroll
    def "Arguments can be joined to take quotes into consideration"() {
        when: 'When the example arguments are joined taking quotes into consideration'
        def result = CommandHelper.breakupArguments( args )

        then: 'Arguments broken up by spaces are joined together if there were quotes around them'
        result == expectedResult as String[]

        where:
        args                    | expectedResult
        ''                      | [ ]
        'abc'                   | [ 'abc' ]
        'ab c'                  | [ 'ab', 'c' ]
        '  a   b   '            | [ 'a', 'b' ]
        'ab "c"'                | [ 'ab', 'c' ]
        'ab " c "'              | [ 'ab', ' c ' ]
        'ab " c  "  '           | [ 'ab', ' c  ' ]
        'ab " c c" "d" e "f"'   | [ 'ab', ' c c', 'd', 'e', 'f' ]
        'ab \\"cd\\"'           | [ 'ab', '"cd"' ]
        'ab "c\\"d e\\"" "f g"' | [ 'ab', 'c"d e"', 'f g' ]
        'ab "c\\"d e" "\\"f g"' | [ 'ab', 'c"d e', '"f g' ]
        'ab\\\\c \\"d'          | [ 'ab\\c', '"d' ]
    }

}
