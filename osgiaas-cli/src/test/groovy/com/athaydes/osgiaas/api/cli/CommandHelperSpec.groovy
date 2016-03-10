package com.athaydes.osgiaas.api.cli

import spock.lang.Specification
import spock.lang.Unroll

class CommandHelperSpec extends Specification {

    @Unroll
    def "Arguments can be joined to take quotes into consideration"() {
        when: 'When the example arguments are joined taking quotes into consideration'
        def result = CommandHelper.breakupArguments( args )

        then: 'Arguments broken up by spaces are joined together if there were quotes around them'
        result == expectedResult

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

    @Unroll
    def "A maximum number of arguments can be split, leaving rest of command intact"() {
        when: 'When a maximum number of arguments are split from an example command'
        def resultArgs = [ ]
        def unprocessed = CommandHelper.breakupArguments( args ) { arg ->
            resultArgs << arg
            resultArgs.size() < limit
        }

        then: 'Arguments broken up by spaces are joined together if there were quotes around them'
        resultArgs == expectedResult

        and: 'only part of the input was processed'
        unprocessed == expectedUnprocessed

        where:
        args              | limit | expectedResult                     | expectedUnprocessed
        ''                | 0     | [ ]                                | ''
        ''                | 1     | [ ]                                | ''
        'a'               | 1     | [ 'a' ]                            | ''
        'a b'             | 1     | [ 'a' ]                            | 'b'
        'a b'             | 2     | [ 'a', 'b' ]                       | ''
        'a b c'           | 2     | [ 'a', 'b' ]                       | 'c'
        'a "b c" d e'     | 0     | [ ]                                | 'a "b c" d e'
        'a "b c" d e'     | 1     | [ 'a' ]                            | '"b c" d e'
        'a "b c" d e'     | 2     | [ 'a', 'b c' ]                     | 'd e'
        'a "b c" d e'     | 3     | [ 'a', 'b c', 'd' ]                | 'e'
        'a "b c" d e'     | 4     | [ 'a', 'b c', 'd', 'e' ]           | ''
        'a "b c" d e'     | 5     | [ 'a', 'b c', 'd', 'e' ]           | ''
        'a "b c" d e f g' | 1     | [ 'a' ]                            | '"b c" d e f g'
        '"a b c" d e f g' | 2     | [ 'a b c', 'd' ]                   | 'e f g'
        '"a b c" d e f g' | 3     | [ 'a b c', 'd', 'e' ]              | 'f g'
        'a "b c" d e f g' | 12345 | [ 'a', 'b c', 'd', 'e', 'f', 'g' ] | ''
    }

}
