package com.athaydes.osgiaas.cli

import com.athaydes.osgiaas.api.cli.OsgiaasCommand
import com.athaydes.osgiaas.api.stream.LineOutputStream
import spock.lang.Specification

class OsgiaasShellSpec extends Specification {

    def shell = new OsgiaasShell()

    def "OSGiaaS Shell can execute simple commands"() {
        given: 'a simple command is added to the shell'
        def commandReceiver = [ ]
        def command = mockCommand( commandReceiver, 'hello simple' )
        shell.addService( command )

        and: 'mocked out streams'
        def output = [ ]
        def out = new LineOutputStream( output.&add )
        def errorOut = [ ]
        def err = new LineOutputStream( errorOut.&add )

        when: 'the command is executed'
        shell.executePiped( [ new OsgiaasShell.Cmd( command, '' ) ] as LinkedList<OsgiaasShell.Cmd>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the Out stream gets the text printed by the simple command'
        output == [ 'hello simple' ]

        and: 'the error stream does not receive any input'
        errorOut.empty

        and: 'the command receives no input'
        commandReceiver.empty
    }

    def "OSGiaaS Shell can execute piped commands"() {
        given: 'a few simple commands are added to the shell'
        def receiver1 = [ ]
        def command1 = mockCommand( receiver1, 'c1' )

        def receiver2 = [ ]
        def command2 = mockCommand( receiver2, 'c2', 'c22' )

        def receiver3 = [ ]
        def command3 = mockCommand( receiver3, 'c3', 'again c3' )

        shell.addService( command1 )
        shell.addService( command2 )
        shell.addService( command3 )

        and: 'mocked out output streams'
        def output = [ ]
        def out = new LineOutputStream( output.&add )
        def errorOut = [ ]
        def err = new LineOutputStream( errorOut.&add )

        when: 'the commands are executed piped'
        shell.executePiped( [
                new OsgiaasShell.Cmd( command1, '' ),
                new OsgiaasShell.Cmd( command2, '' ),
                new OsgiaasShell.Cmd( command3, '' )
        ] as LinkedList<OsgiaasShell.Cmd>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the OutputStream gets the text printed by the last command'
        output == [ 'c3', 'again c3' ]

        and: 'the commands get the expected streamed input'
        receiver1 == [ ]
        receiver2 == [ 'c1' ]
        receiver3 == [ 'c2', 'c22' ]
    }

    private OsgiaasCommand mockCommand( List receiver, String... linesToPrint ) {
        Stub( OsgiaasCommand ) {
            //noinspection GroovyAssignabilityCheck
            pipe( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                linesToPrint.each( out.&println )
                return receiver.&add
            }
            getName() >> UUID.randomUUID().toString()
        }
    }

}
