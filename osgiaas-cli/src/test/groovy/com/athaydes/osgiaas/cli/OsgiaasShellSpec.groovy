package com.athaydes.osgiaas.cli

import com.athaydes.osgiaas.api.cli.CommandModifier
import com.athaydes.osgiaas.api.cli.StreamingCommand
import com.athaydes.osgiaas.api.stream.LineOutputStream
import org.apache.felix.shell.Command
import spock.lang.Specification

import javax.annotation.Nullable

class OsgiaasShellSpec extends Specification {

    def "OSGiaaS Shell can execute a simple command"() {
        given: 'a OSGiaaS Shell with a simple command'
        def command = Stub( Command ) {
            getName() >> 'simple'
            //noinspection GroovyAssignabilityCheck
            execute( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                out.println 'hello: ' + line
                out.println 'done'
                err.println 'all good'
            }
        }

        def shell = new OsgiaasShell( { [ command ] as Set }, { [ ] as Set } )

        and: 'mocked out streams'
        def outClosable = Mock( AutoCloseable )
        def output = [ ]
        def out = new LineOutputStream( output.&add, outClosable )

        def errClosable = Mock( AutoCloseable )
        def errorOut = [ ]
        def err = new LineOutputStream( errorOut.&add, errClosable )

        when: 'the command is executed'
        shell.runCommand( 'simple is great',
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the Out stream gets the text printed by the simple command'
        output == [ 'hello: simple is great', 'done' ]

        and: 'the error stream prints what the command requested'
        errorOut == [ 'all good' ]
    }

    def "OSGiaaS Shell can execute a simple command with command modifiers"() {
        given: 'a OSGiaaS Shell with a few simple commands'
        def xhello = mockSimpleCommand( 'xhello', 'xx' )
        def yhello = mockSimpleCommand( 'yhello', 'yy' )

        and: 'A command modifier'
        def modifier = { String cmd ->
            if ( cmd ==~ 'hello.*' ) [ 'x' + cmd, 'y' + cmd ] else [ cmd ]
        } as CommandModifier

        def shell = new OsgiaasShell( { [ xhello, yhello ] as Set }, { [ modifier ] as Set } )

        and: 'mocked out streams'
        def outClosable = Mock( AutoCloseable )
        def output = [ ]
        def out = new LineOutputStream( output.&add, outClosable )

        def errClosable = Mock( AutoCloseable )
        def errorOut = [ ]
        def err = new LineOutputStream( errorOut.&add, errClosable )

        when: 'the command is executed'
        shell.runCommand( 'hello 123',
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the error stream does not print anything'
        errorOut == [ ]

        and: 'the Out stream gets the text printed by the modified simple commands'
        output == [ 'xx', 'yy' ]
    }

    def "OSGiaaS Shell can execute piped simple commands"() {
        given: 'a few simple Commands are added to the shell'
        def command1 = mockSimpleCommand( null, 'c1' )
        def command2 = mockSimpleCommand( null, 'c2', 'c22' )
        def command3 = mockSimpleCommand( null, 'c3', 'again c3' )

        def shell = new OsgiaasShell( { [ command1, command2, command3 ] as Set }, { [ ] as Set } )

        and: 'mocked out output streams'
        def outClosable = Mock( AutoCloseable )
        def output = [ ]
        def out = new LineOutputStream( output.&add, outClosable )
        def errClosable = Mock( AutoCloseable )
        def errorOut = [ ]
        def err = new LineOutputStream( errorOut.&add, errClosable )

        when: 'the commands are executed piped'
        shell.executePiped( [
                [ new OsgiaasShell.Cmd( command1, '' ) ],
                [ new OsgiaasShell.Cmd( command2, '' ) ],
                [ new OsgiaasShell.Cmd( command3, '' ) ]
        ] as LinkedList<List<OsgiaasShell.Cmd>>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the OutputStream gets the text printed by the last command'
        output == [ 'c3', 'again c3' ]

        and: 'no error is printed'
        errorOut.empty
    }

    def "OSGiaaS Shell can execute piped streaming commands"() {
        given: 'a few StreamingCommands are added to the shell'
        def receiver1 = [ ]
        def command1 = mockStreamingCommand( receiver1, 'c1' )

        def receiver2 = [ ]
        def command2 = mockStreamingCommand( receiver2, 'c2', 'c22' )

        def receiver3 = [ ]
        def command3 = mockStreamingCommand( receiver3, 'c3', 'again c3' )

        def shell = new OsgiaasShell( { [ command1, command2, command3 ] as Set }, { [ ] as Set } )

        and: 'mocked out output streams'
        def outClosable = Mock( AutoCloseable )
        def output = [ ]
        def out = new LineOutputStream( output.&add, outClosable )
        def errClosable = Mock( AutoCloseable )
        def errorOut = [ ]
        def err = new LineOutputStream( errorOut.&add, errClosable )

        when: 'the commands are executed piped'
        shell.executePiped( [
                [ new OsgiaasShell.Cmd( command1, '' ) ],
                [ new OsgiaasShell.Cmd( command2, '' ) ],
                [ new OsgiaasShell.Cmd( command3, '' ) ]
        ] as LinkedList<List<OsgiaasShell.Cmd>>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the OutputStream gets the text printed by the last command'
        output == [ 'c3', 'again c3' ]

        and: 'the commands get the expected streamed input'
        receiver1 == [ ]
        receiver2 == [ 'c1' ]
        receiver3 == [ 'c2', 'c22' ]

        and: 'no error is printed'
        errorOut.empty
    }

    def "OSGiaaS Shell can execute mixed piped streaming and simple commands"() {
        given: 'a Streaming, simple and Streaming commands are added to the shell'
        def receiver1 = [ ]
        def command1 = mockStreamingCommand( receiver1, 'c1' )

        def command2 = mockSimpleCommand( null, 'c2', 'c22' )

        def receiver3 = [ ]
        def command3 = mockStreamingCommand( receiver3, 'c3', 'again c3' )

        def shell = new OsgiaasShell( { [ command1, command2, command3 ] as Set }, { [ ] as Set } )

        and: 'mocked out output streams'
        def outClosable = Mock( AutoCloseable )
        def output = [ ]
        def out = new LineOutputStream( output.&add, outClosable )
        def errClosable = Mock( AutoCloseable )
        def errorOut = [ ]
        def err = new LineOutputStream( errorOut.&add, errClosable )

        when: 'the commands are executed piped'
        shell.executePiped( [
                [ new OsgiaasShell.Cmd( command1, '' ) ],
                [ new OsgiaasShell.Cmd( command2, '' ) ],
                [ new OsgiaasShell.Cmd( command3, '' ) ]
        ] as LinkedList<List<OsgiaasShell.Cmd>>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the OutputStream gets the text printed by the last command'
        output == [ 'c3', 'again c3' ]

        and: 'the commands get the expected streamed input'
        receiver1 == [ ]
        receiver3 == [ 'c2', 'c22' ]

        and: 'no error is printed'
        errorOut.empty
    }

    def "CommandModifiers are applied in turn, and added commands are also transformed"() {
        given: 'A set of known command modifiers'
        def modifiers = [
                { cmd -> if ( cmd == 'hi' ) [ cmd ] else [ cmd, 'hi' ] },
                { cmd -> [ cmd + '$' ] }
        ].collect { it as CommandModifier }

        when: 'A command is transformed with the modifiers'
        def commands = OsgiaasShell.transformCommand( command, modifiers )

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
        def commands = OsgiaasShell.transformCommand( command, modifiers )

        then: 'The expected commands are returned'
        commands == expectedResult

        where:
        command   | expectedResult
        'command' | [ 'command$' ]
        'a'       | [ 'a$' ]
        'hi'      | [ ]
    }

    private StreamingCommand mockStreamingCommand( List receiver, String... linesToPrint ) {
        Stub( StreamingCommand ) {
            //noinspection GroovyAssignabilityCheck
            pipe( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                linesToPrint.each( out.&println )
                return new LineOutputStream( receiver.&add, out )
            }
            execute( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                linesToPrint.each( out.&println )
            }
            getName() >> UUID.randomUUID().toString()
        }
    }

    private Command mockSimpleCommand( @Nullable String name, String... linesToPrint ) {
        Stub( Command ) {
            //noinspection GroovyAssignabilityCheck
            execute( _, _, _ ) >> { String cmd, PrintStream out, PrintStream err ->
                for ( line in linesToPrint ) out.println( line )
            }
            getName() >> ( name ?: UUID.randomUUID().toString() )
        }
    }

}
