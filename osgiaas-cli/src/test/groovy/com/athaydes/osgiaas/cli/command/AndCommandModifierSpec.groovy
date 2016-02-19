package com.athaydes.osgiaas.cli.command

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AndCommandModifierSpec extends Specification {

    @Subject
    final command = new AndCommandModifier()

    @Unroll
    def "AndCommandModifier can split commands correctly"() {
        when: ''
        def result = command.apply( line )

        then:
        result == expectedResult

        where:
        line                 | expectedResult
        ''                   | [ '' ]
        'a'                  | [ 'a' ]
        'a b c'              | [ 'a b c' ]
        'a && b'             | [ 'a', 'b' ]
        'a&&b'               | [ 'a&&b' ]
        'a && b && c d && e' | [ 'a', 'b', 'c d', 'e' ]
        'a && b&&c d && e'   | [ 'a', 'b&&c d', 'e' ]
    }

}
