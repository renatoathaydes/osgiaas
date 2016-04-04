package com.athaydes.osgiaas.api.cli.completer

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class BaseCompleterSpec extends Specification {

    def "BaseCompleter can complete simple commands arguments"() {
        given: "A simple command completer based on BaseCompleter"
        def completer = new BaseCompleter( CompletionMatcher.nameMatcher( 'cmd', [
                CompletionMatcher.nameMatcher( 'opt1' ),
                CompletionMatcher.nameMatcher( 'something' ),
                CompletionMatcher.nameMatcher( 'other' ),
        ] ) )

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
        def completer = new BaseCompleter( CompletionMatcher.nameMatcher( 'cmd', [
                CompletionMatcher.nameMatcher( 'opt1', [
                        CompletionMatcher.nameMatcher( 'def', [
                                CompletionMatcher.nameMatcher( '123' ),
                                CompletionMatcher.nameMatcher( '456' ),
                        ] ),
                        CompletionMatcher.nameMatcher( 'ghi', [
                                CompletionMatcher.nameMatcher( 'xyz' )
                        ] ),
                        CompletionMatcher.nameMatcher( 'jkl' )
                ] ),
                CompletionMatcher.nameMatcher( 'something' ),
                CompletionMatcher.nameMatcher( 'other', [
                        CompletionMatcher.nameMatcher( 'o1' ),
                        CompletionMatcher.nameMatcher( 'o2', [
                                CompletionMatcher.nameMatcher( 'o2.1' ),
                                CompletionMatcher.nameMatcher( 'o2.2' ),
                        ] ),
                ] ),
        ] ) )

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
        def completer = new BaseCompleter( CompletionMatcher.nameMatcher( 'cmd', [
                CompletionMatcher.multiPartMatcher( '-', [
                        [ 'p1', 'p2', 'p3' ],
                        [ 'queue', 'row', 'seat' ],
                        [ 'table' ]
                ] ),
                CompletionMatcher.nameMatcher( 'something' ),
                CompletionMatcher.nameMatcher( 'other' ),
        ] ) )

        when: "Some example user commands are queried for completion"
        def candidates = [ ]
        def index = completer.complete( command, cursor, candidates )

        then: "The expected completions are added"
        candidates == expectedCandidates

        and: "The expected index is returned"
        index == expectedIndex

        where:
        command         | cursor | expectedIndex | expectedCandidates
        'cmd '          | 4      | 4             | [ 'p1', 'p2', 'p3', 'something', 'other' ]
        'cmd p'         | 5      | 4             | [ 'p1', 'p2', 'p3' ]
        'cmd p1'        | 6      | 4             | [ 'p1-queue', 'p1-row', 'p1-seat' ]
        'cmd p1-'       | 7      | 4             | [ 'p1-queue', 'p1-row', 'p1-seat' ]
        'cmd p1-r'      | 8      | 4             | [ 'p1-row' ]
        'cmd p1-row'    | 10     | 4             | [ 'p1-row-table' ]
        'cmd p1-row-'   | 11     | 4             | [ 'p1-row-table' ]
        'cmd p1-row-ta' | 13     | 4             | [ 'p1-row-table' ]
        'cmd p1-row-x'  | 12     | -1            | [ ]
        'cmd p2-r'      | 8      | 4             | [ 'p2-row' ]
        'cmd p3-s'      | 8      | 4             | [ 'p3-seat' ]
        'cmd p3-seat'   | 11     | 4             | [ 'p3-seat-table' ]
        'cmd p3-seat-'  | 12     | 4             | [ 'p3-seat-table' ]
    }

}