package com.athaydes.osgiaas.cli.core.command

import com.athaydes.osgiaas.api.stream.LineOutputStream
import spock.lang.Specification
import spock.lang.Unroll

class GrepCommandSpec extends Specification {

    @Unroll
    def "GrepCall is created correctly from a command line"() {
        when: 'Parsing a valid grep call'
        def result = new GrepCommand().grepCall( line, new PrintStream( Stub( OutputStream ) ) )

        then: 'The grep call is recognized as valid'
        result != null

        and: 'All parameters are interpreted correctly'
        result.beforeLines == before
        result.beforeGiven == beforeGiven
        result.afterLines == after
        result.afterGiven == afterGiven
        result.regex == regex
        result.text == text
        result.caseInsensitive == caseInsensitive

        where:
        line                    | before | after | beforeGiven | afterGiven | regex          | text  | caseInsensitive
        'grep a b'              | 0      | 0     | false       | false      | 'a'            | 'b'   | false
        'grep a b c'            | 0      | 0     | false       | false      | 'a'            | 'b c' | false
        'grep -B 2 a b'         | 2      | 0     | true        | false      | 'a'            | 'b'   | false
        'grep -B 2 -A 6 a b'    | 2      | 6     | true        | true       | 'a'            | 'b'   | false
        'grep -B 2 -A 6 -i a b' | 2      | 6     | true        | true       | 'a'            | 'b'   | true
        'grep -A 2 a b'         | 0      | 2     | false       | true       | 'a'            | 'b'   | false
        'grep -A 2 -B 3 a b'    | 3      | 2     | true        | true       | 'a'            | 'b'   | false
        'grep -b x'             | 0      | 0     | false       | false      | '-b'           | 'x'   | false
        'grep -i -b x'          | 0      | 0     | false       | false      | '-b'           | 'x'   | true
        'grep  -A 3 -B 1 hi'    | 1      | 3     | true        | true       | 'hi'           | ''    | false
        'grep  -B 1 -A 3 hi'    | 1      | 3     | true        | true       | 'hi'           | ''    | false
        'grep rx'               | 0      | 0     | false       | false      | 'rx'           | ''    | false
        'grep .*a[a-Z]+\\s'     | 0      | 0     | false       | false      | '.*a[a-Z]+\\s' | ''    | false
        'grep -i .*a[a-Z]+\\s'  | 0      | 0     | false       | false      | '.*a[a-Z]+\\s' | ''    | true
    }

    @Unroll
    def "Invalid grep calls are not recognized"() {
        when: 'An invalid grep call is made'
        def result = new GrepCommand().grepCall( line, new PrintStream( Stub( OutputStream ) ) )

        then: 'The call is not recognized as being valid'
        result == null

        where:
        line << [
                '', 'grep -B', 'grep -A', 'grep -A -B', 'grep -A x', 'grep -B 3 -A f regex text'
        ]
    }

    @Unroll
    def "Can grep text as expected"() {
        given: 'A sample text'
        def text = '''\
            |abc
            |def
            |ghi
            |abcdefghi'''.stripMargin()

        when: 'We grep the text using the regex = #regex'
        def result = [ ]
        def errors = [ ]
        new GrepCommand().grepAndConsume( "grep -B $before -A $after $regex $text",
                new PrintStream( new LineOutputStream( result.&add, { -> } ) ),
                new PrintStream( new LineOutputStream( errors.&add, { -> } ) ) )

        then: 'The selected lines are as expected'
        result == expectedResult

        and: 'No errors are printed'
        errors.empty

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
        'd'   | 1      | 0     | [ 'abc', 'def', 'ghi', 'abcdefghi' ]
        'd'   | 2      | 2     | [ 'abc', 'def', 'ghi', 'abcdefghi' ]
        'c$'  | 2      | 2     | [ 'abc', 'def', 'ghi' ]
        'cd'  | 2      | 2     | [ 'def', 'ghi', 'abcdefghi' ]
    }

    @Unroll
    def "Can grep text using case-insensitive option as expected"() {
        given: 'A sample text'
        def text = '''\
            |abc
            |ABC
            |def
            |ABCabc'''.stripMargin()

        when: 'We grep the text using the case insensitive option the regex = #regex'
        def result = [ ]
        def errors = [ ]
        new GrepCommand().grepAndConsume( "grep -i $regex $text",
                new PrintStream( new LineOutputStream( result.&add, { -> } ) ),
                new PrintStream( new LineOutputStream( errors.&add, { -> } ) ) )

        then: 'The selected lines are as expected'
        result == expectedResult

        and: 'No errors are printed'
        errors.empty

        where:
        regex | expectedResult
        'x'   | [ ]
        'A'   | [ 'abc', 'ABC', 'ABCabc' ]
        'b'   | [ 'abc', 'ABC', 'ABCabc' ]
        'c$'  | [ 'abc', 'ABC', 'ABCabc' ]
        'd'   | [ 'def' ]
        'DEF' | [ 'def' ]
    }

    @Unroll
    def "Grep can be called without immediate text input"() {
        when: 'the grep command is called with some options, but no immediate text input'
        def result = [ ]
        def errors = [ ]
        def consumer = new GrepCommand().grepAndConsume( "grep $args",
                new PrintStream( new LineOutputStream( result.&add, { -> } ) ),
                new PrintStream( new LineOutputStream( errors.&add, { -> } ) ) )

        then: 'No output or error is sent out'
        result.empty
        errors.empty

        when: 'some input is provided via the grep consumer'
        consumer.accept( lateInput )

        then: 'the expected output is sent out'
        result == expectedOutput

        and: 'no errors are reported'
        errors.empty

        where:
        args              | lateInput | expectedOutput
        'regex'           | 'hi'      | [ ]
        'hi'              | 'hi'      | [ 'hi' ]
        '-B 1 regex'      | 'hi'      | [ ]
        '-B 1 hi'         | 'hi'      | [ 'hi' ]
        '-B 1 -A 2 regex' | 'hi'      | [ ]
        '-B 1 -A 2 hi'    | 'hi'      | [ 'hi' ]
        '-A 3 -B 1 regex' | 'hi'      | [ ]
        '-A 3 -B 1 hi'    | 'hi'      | [ 'hi' ]
        '-A 4 regex'      | 'hi'      | [ ]
        '-A 4 hi'         | 'hi'      | [ 'hi' ]
    }

}
