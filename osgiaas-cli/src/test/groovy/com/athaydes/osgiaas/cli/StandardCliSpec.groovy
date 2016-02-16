package com.athaydes.osgiaas.cli

import com.athaydes.osgiaas.api.cli.CommandModifier
import spock.lang.Specification

class StandardCliSpec extends Specification {

    def "CommandModifiers are applied in turn, and added commands are also transformed"() {
        given: 'A set of known command modifiers'
        def modifiers = [
                { cmd -> if ( cmd == 'hi' ) [ cmd ] else [ cmd, 'hi' ] },
                { cmd -> [ cmd + '$' ] }
        ].collect { it as CommandModifier }

        when: 'A command is transformed with the modifiers'
        def commands = StandardCli.transformCommand( command, modifiers )

        then: 'The expected commands are returned'
        commands == expectedResult

        where:
        command   | expectedResult
        'command' | [ 'command$', 'hi$' ]
        'a'       | [ 'a$', 'hi$' ]
    }

    def "CommandModifiers are applied in turn, and transformers may remove commands"() {
        given: 'A set of known command modifiers'
        def modifiers = [
                { cmd -> if ( cmd == 'hi' ) [ ] else [ cmd ] },
                { cmd -> [ cmd + '$' ] }
        ].collect { it as CommandModifier }

        when: 'A command is transformed with the modifiers'
        def commands = StandardCli.transformCommand( command, modifiers )

        then: 'The expected commands are returned'
        commands == expectedResult

        where:
        command   | expectedResult
        'command' | [ 'command$' ]
        'a'       | [ 'a$' ]
        'hi'      | [ ]
    }


}
