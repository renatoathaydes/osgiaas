package com.athaydes.osgiaas.cli.command

import spock.lang.Specification
import spock.lang.Unroll

class GrepCommandSpec extends Specification {

    @Unroll
    def "GrepCall is created correctly from a command line"() {
        when: 'Parsing a valid grep call'
        def result = GrepCommand.grepCall( line )

        then: 'The grep call is recognized as valid'
        result != null

        and: 'All parameters are interpreted correctly'
        result.beforeLines == before
        result.beforeGiven == beforeGiven
        result.afterLines == after
        result.afterGiven == afterGiven

        where:
        line                 | before | after | beforeGiven | afterGiven
        'grep a b'           | 0      | 0     | false       | false
        'grep a b c'         | 0      | 0     | false       | false
        'grep -B 2 a b'      | 2      | 0     | true        | false
        'grep -B 2 -A 6 a b' | 2      | 6     | true        | true
        'grep -A 2 a b'      | 0      | 2     | false       | true
        'grep -A 2 -B 3 a b' | 3      | 2     | true        | true
        'grep -A b'          | 0      | 0     | false       | false
        'grep -B b'          | 0      | 0     | false       | false
        'grep -B -A b'       | 0      | 0     | false       | false
        'grep -b x'          | 0      | 0     | false       | false
    }

    @Unroll
    def "Invalid grep calls are not recognized"() {
        when: 'An invalid grep call is made'
        def result = GrepCommand.grepCall( line )

        then: 'The call is not recognized as being valid'
        result == null

        where:
        line << [
                '', 'abc', 'grepme', 'grep', 'grep abc'
        ]
    }

    @Unroll
    def "Can grep text as expected"() {
        given: 'a GrepCall with beforeLines = #before and afterLines = #after'
        def grepCall = new GrepCommand.GrepCall( before, true, after, true )

        and: 'A sample text'
        def text = '''\
            |abc
            |def
            |ghi
            |abcdefghi'''.stripMargin()

        when: 'We grep using the regex = #regex'
        def result = []
        GrepCommand.grep( regex, text, grepCall, result.&add )

        then: 'The selected lines are as expected'
        result == expectedResult

        where:
        regex | before | after | expectedResult
        'x'   | 0      | 0     | [ ]
        'a'   | 0      | 0     | [ 'abc', 'abcdefghi' ]
        'b'   | 0      | 0     | [ 'abc', 'abcdefghi' ]
        'c$'  | 0      | 0     | [ 'abc' ]
        'd'   | 0      | 0     | [ 'def', 'abcdefghi' ]
        'i'   | 0      | 0     | [ 'ghi', 'abcdefghi' ]
        'cd'  | 0      | 0     | [ 'abcdefghi' ]
        'a'   | 1      | 0     | [ 'abc', 'ghi', 'abcdefghi' ]
        'a'   | 0      | 1     | [ 'abc', 'def', 'abcdefghi' ]
        'd'   | 1      | 1     | [ 'abc', 'def', 'ghi', 'abcdefghi' ]
        'd'   | 2      | 2     | [ 'abc', 'def', 'ghi', 'abcdefghi' ]
        'c$'  | 2      | 2     | [ 'abc', 'def', 'ghi' ]
        'cd'  | 2      | 2     | [ 'def', 'ghi', 'abcdefghi' ]
    }

}
