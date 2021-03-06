package com.athaydes.osgiaas.cli.core.completer

import com.athaydes.osgiaas.cli.completer.AnyLevelMatcher
import com.athaydes.osgiaas.cli.completer.BaseCompleter
import com.athaydes.osgiaas.cli.completer.CompletionMatcher
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Stream

import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.alternativeMatchers
import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.nameMatcher

@Unroll
class BaseCompleterSpec extends Specification {

    def "BaseCompleter can complete simple commands arguments"() {
        given: "A simple command completer based on BaseCompleter"
        def completer = new BaseCompleter( nameMatcher( 'cmd', [
                nameMatcher( 'opt1' ),
                nameMatcher( 'something' ),
                nameMatcher( 'other' ),
        ] as CompletionMatcher[] ) )

        when: "Some example user commands are queried for completion"
        def candidates = [ ]
        def index = completer.complete( command, cursor, candidates )

        then: "The expected completions are added"
        candidates == expectedCandidates

        and: "The expected index is returned"
        index == expectedIndex

        where:
        command          | cursor | expectedIndex | expectedCandidates
        'cmd '           | 4      | 4             | [ 'opt1', 'something', 'other' ]
        'cmd o'          | 5      | 4             | [ 'opt1', 'other' ]
        'cmd some'       | 8      | 4             | [ 'something' ]
        'cmd some'       | 4      | 4             | [ 'opt1', 'something', 'other' ]
        'cmd oh my god'  | 5      | 4             | [ 'opt1', 'other' ]
        'cmd x'          | 5      | -1            | [ ]
        'cmd opt1not'    | 9      | -1            | [ ]
        'cmd opt1 '      | 9      | -1            | [ ]
        'cmd something ' | 14     | -1            | [ ]
    }

    def "BaseCompleter can complete complex commands arguments"() {
        given: "A simple command completer based on BaseCompleter"
        def completer = new BaseCompleter( nameMatcher( 'cmd', [
                nameMatcher( 'opt1', [
                        nameMatcher( 'def', [
                                nameMatcher( '123' ),
                                nameMatcher( '456' ),
                        ] as CompletionMatcher[] ),
                        nameMatcher( 'ghi', [
                                nameMatcher( 'xyz' )
                        ] as CompletionMatcher[] ),
                        nameMatcher( 'jkl' )
                ] as CompletionMatcher[] ),
                nameMatcher( 'something' ),
                nameMatcher( 'other', [
                        nameMatcher( 'o1' ),
                        nameMatcher( 'o2', [
                                nameMatcher( 'o2.1' ),
                                nameMatcher( 'o2.2' ),
                        ] as CompletionMatcher[] ),
                ] as CompletionMatcher[] ),
        ] as CompletionMatcher[] ) )

        when: "Some example user commands are queried for completion"
        def candidates = [ ]
        def index = completer.complete( command, cursor, candidates )

        then: "The expected completions are added"
        candidates == expectedCandidates

        and: "The expected index is returned"
        index == expectedIndex

        where:
        command             | cursor | expectedIndex | expectedCandidates
        'cmd '              | 4      | 4             | [ 'opt1', 'something', 'other' ]
        'cmd o'             | 5      | 4             | [ 'opt1', 'other' ]
        'cmd some'          | 8      | 4             | [ 'something' ]
        'cmd some'          | 4      | 4             | [ 'opt1', 'something', 'other' ]
        'cmd oh my god'     | 5      | 4             | [ 'opt1', 'other' ]
        'cmd x'             | 5      | -1            | [ ]
        'cmd opt1not'       | 9      | -1            | [ ]
        'cmd opt1 '         | 9      | 9             | [ 'def', 'ghi', 'jkl' ]
        'cmd something '    | 14     | -1            | [ ]
        'cmd other '        | 10     | 10            | [ 'o1', 'o2' ]
        'cmd opt1 d'        | 10     | 9             | [ 'def' ]
        'cmd opt1 def '     | 13     | 13            | [ '123', '456' ]
        'cmd opt1 def 1'    | 14     | 13            | [ '123' ]
        'cmd opt1 def 123 ' | 17     | -1            | [ ]
        'cmd opt1 def 4'    | 14     | 13            | [ '456' ]
        'cmd something o'   | 15     | -1            | [ ]
        'cmd other o'       | 11     | 10            | [ 'o1', 'o2' ]
        'cmd other o2'      | 12     | 10            | [ 'o2' ]
        'cmd other o2 '     | 13     | 13            | [ 'o2.1', 'o2.2' ]
        'cmd other o1 '     | 13     | -1            | [ ]
        'cmd other o2 o2'   | 15     | 13            | [ 'o2.1', 'o2.2' ]
        'cmd other o2 o2.'  | 16     | 13            | [ 'o2.1', 'o2.2' ]
        'cmd other o2 o2.1' | 17     | 13            | [ 'o2.1' ]
        'cmd other o2 o3.'  | 16     | -1            | [ ]
    }

    def "BaseCompleter can complete multi-part commands arguments"() {
        given: "A simple command completer based on BaseCompleter"
        def asMatcher = { String s -> nameMatcher( s ) }
        def alternatives = { Collection<CompletionMatcher> matchers ->
            alternativeMatchers( matchers.&stream )
        }
        def completer = new BaseCompleter( nameMatcher( 'cmd', [
                CompletionMatcher.multiPartMatcher( '-', [
                        alternatives( [ 'p1', 'p2', 'p3' ].collect( asMatcher ) ),
                        alternatives( [ 'queue', 'row', 'seat' ].collect( asMatcher ) ),
                        nameMatcher( 'table' )
                ], {
                    Stream.of( CompletionMatcher.multiPartMatcher( '+', [
                            alternatives( [ 'abc', 'def' ].collect( asMatcher ) ),
                            alternatives( [ 'ghi', 'jkl' ].collect( asMatcher ) )
                    ], { Stream.empty() } ) )
                } ),
                nameMatcher( 'something' ),
                nameMatcher( 'other' ),
        ] as CompletionMatcher[] ) )

        when: "Some example user commands are queried for completion"
        def candidates = [ ]
        def index = completer.complete( command, cursor, candidates )

        then: "The expected completions are added"
        candidates == expectedCandidates

        and: "The expected index is returned"
        index == expectedIndex

        where:
        command                   | cursor | expectedIndex | expectedCandidates
        'cmd '                    | 4      | 4             | [ 'p1', 'p2', 'p3', 'something', 'other' ]
        'cmd p'                   | 5      | 4             | [ 'p1', 'p2', 'p3' ]
        'cmd p1'                  | 6      | 4             | [ 'p1-queue', 'p1-row', 'p1-seat' ]
        'cmd p1-'                 | 7      | 4             | [ 'p1-queue', 'p1-row', 'p1-seat' ]
        'cmd p1-r'                | 8      | 4             | [ 'p1-row' ]
        'cmd p1-row'              | 10     | 4             | [ 'p1-row-table' ]
        'cmd p1-row-'             | 11     | 4             | [ 'p1-row-table' ]
        'cmd p1-row-ta'           | 13     | 4             | [ 'p1-row-table' ]
        'cmd p1-row-x'            | 12     | -1            | [ ]
        'cmd p2-r'                | 8      | 4             | [ 'p2-row' ]
        'cmd p3-s'                | 8      | 4             | [ 'p3-seat' ]
        'cmd p3-seat'             | 11     | 4             | [ 'p3-seat-table' ]
        'cmd p3-seat-'            | 12     | 4             | [ 'p3-seat-table' ]
        'cmd p3-seat-table '      | 18     | 18            | [ 'abc', 'def' ]
        'cmd p3-seat-table a'     | 19     | 18            | [ 'abc' ]
        'cmd p3-seat-table d'     | 19     | 18            | [ 'def' ]
        'cmd p3-seat-table def'   | 21     | 18            | [ 'def+ghi', 'def+jkl' ]
        'cmd p3-seat-table def+'  | 22     | 18            | [ 'def+ghi', 'def+jkl' ]
        'cmd p3-seat-table def+j' | 23     | 18            | [ 'def+jkl' ]
        'cmd p3-seat-table z'     | 19     | -1            | [ ]
        'cmd p3-seat-table def-'  | 22     | -1            | [ ]
        'cmd p3-seat-table def+z' | 23     | -1            | [ ]
    }

    def "BaseCompleter can complete at any level using DelegateMatcher"() {
        given:
        def completer = new BaseCompleter( new AnyLevelMatcher(
                alternativeMatchers(
                        nameMatcher( "abcde" ),
                        nameMatcher( "xyz" ),
                        nameMatcher( "abxyz" )
                ) ) )

        when: "Some example user commands are queried for completion"
        def candidates = [ ]
        def index = completer.complete( command, cursor, candidates )

        then: "The expected completions are added"
        candidates == expectedCandidates

        and: "The expected index is returned"
        index == expectedIndex

        where:
        command           | cursor | expectedIndex | expectedCandidates
        'cmd '            | 4      | 4             | [ 'abcde', 'xyz', 'abxyz' ]
        'cmd a'           | 5      | 4             | [ 'abcde', 'abxyz' ]
        'cmd abc'         | 7      | 4             | [ 'abcde' ]
        'cmd x'           | 5      | 4             | [ 'xyz' ]
        'cmd p'           | 5      | -1            | [ ]
        'cmd xxx '        | 8      | 8             | [ 'abcde', 'xyz', 'abxyz' ]
        'cmd xxx x'       | 9      | 8             | [ 'xyz' ]
        'cmd xxx mm nn x' | 15     | 14            | [ 'xyz' ]
        'cmd xxx mm nn '  | 14     | 14            | [ 'abcde', 'xyz', 'abxyz' ]
    }

}