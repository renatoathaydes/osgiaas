package com.athaydes.osgiaas.cli.completer

import com.athaydes.osgiaas.api.cli.CliProperties
import spock.lang.Specification
import spock.lang.Unroll

class OsgiaasCommandCompleterSpec extends Specification {

    @Unroll
    def "OSGiaaS CommandCompleter correctly completes commands"() {
        given: 'A CLI with a few commands available'
        def cliProperties = Stub( CliProperties ) {
            availableCommands() >> [ 'cmd', 'abc', 'def', 'az' ]
        }

        and: 'A OSGiaaSCommandCompleter using the CLI'
        def completer = new OsgiaasCommandCompleter()
        completer.cliProperties = cliProperties

        when: 'The Completer is requested to complete some input'
        def candidates = [ ]
        def index = completer.complete( buffer, cursor, candidates )

        then: 'The list of completions suggested is as expected'
        candidates == expectedSuggestions

        and: 'the returned index of the completion is correct'
        index == expectedIndex

        where:
        buffer          | cursor | expectedSuggestions           | expectedIndex // index
        ''              | 0      | [ 'abc', 'az', 'cmd', 'def' ] | 0             // 0
        'a'             | 0      | [ 'abc', 'az', 'cmd', 'def' ] | 0
        'a'             | 1      | [ 'abc', 'az' ]               | 0
        'c'             | 0      | [ 'abc', 'az', 'cmd', 'def' ] | 0             // 3
        'c'             | 1      | [ 'cmd' ]                     | 0
        'az'            | 2      | [ 'az' ]                      | 0
        'x'             | 1      | [ ]                           | -1            // 6
        ' '             | 1      | [ 'abc', 'az', 'cmd', 'def' ] | 1
        'az '           | 3      | [ ]                           | -1
        'ps|'           | 3      | [ 'abc', 'az', 'cmd', 'def' ] | 3             // 9
        'ps| '          | 4      | [ 'abc', 'az', 'cmd', 'def' ] | 4
        'ps && '        | 6      | [ 'abc', 'az', 'cmd', 'def' ] | 6
        'ps && ls | a'  | 12     | [ 'abc', 'az' ]               | 11            // 12
        'ps && ls|a'    | 10     | [ 'abc', 'az' ]               | 9
        'ps && ls|a '   | 11     | [ ]                           | -1
        'ps && ls|  ab' | 13     | [ 'abc' ]                     | 11            // 15
        'ps && ls  |ab' | 13     | [ 'abc' ]                     | 11
    }

}
