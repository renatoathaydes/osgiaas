package com.athaydes.osgiaas.cli.command

import com.athaydes.osgiaas.api.cli.CliProperties
import spock.lang.Specification
import spock.lang.Subject

class AliasCommandSpec extends Specification {

    @Subject
    final command = new AliasCommand(
            cliProperties: Stub( CliProperties ) {
                availableCommands() >> ( [ 'cmd', 'ls', 'cd', 'grep' ] as String[] )
            } )

    def "Alias command can replace original command with aliased command"() {
        given: 'a number of aliases #aliases'
        command.addAliases( aliases )

        when: 'the AliasCommand is applied to an example command #cmd'
        def result = command.apply( cmd )

        then: 'the alias is used if it exists'
        result == expectedResult

        where:
        aliases                   | cmd             | expectedResult
        [ : ]                     | 'ls'            | [ 'ls' ]
        [ hi: 'ls' ]              | 'ls'            | [ 'ls' ]
        [ hi: 'ls' ]              | 'hi'            | [ 'ls' ]
        [ hi: 'ls' ]              | 'hi there'      | [ 'ls there' ]
        [ hi: 'ls' ]              | 'cd'            | [ 'cd' ]
        [ hi: 'ls' ]              | 'cd hello blah' | [ 'cd hello blah' ]
        [ hi: 'ls', ho: 'hello' ] | 'ho ho ho'      | [ 'hello ho ho' ]
        [ hi: 'ls', ho: 'hello' ] | 'hi ho ho'      | [ 'ls ho ho' ]
        [ hi: 'ls', ho: 'hello' ] | 'ho_ho ho'      | [ 'ho_ho ho' ]
    }

    def "Alias command has no effect if no aliases are added"() {
        given: 'An AliasCommand without aliases'

        when: 'the AliasCommand is applied to an example command #cmd'
        def result = command.apply( cmd )

        then: 'the exact same command is always returned'
        result == [ cmd ]

        where:
        cmd << [ 'ls', 'cmd', 'hi', 'hello world' ]
    }

}
