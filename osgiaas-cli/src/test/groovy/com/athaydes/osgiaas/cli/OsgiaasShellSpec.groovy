package com.athaydes.osgiaas.cli

import com.athaydes.osgiaas.api.cli.OsgiaasCommand
import spock.lang.Specification

class OsgiaasShellSpec extends Specification {

    def shell = new OsgiaasShell()

    def "OSGiaaS Shell can execute simple commands"() {
        given: 'a simple command is added to the shell'
        def commandReceiver = Mock( OutputStream )
        def command = Stub( OsgiaasCommand ) {
            pipe( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                out.println( 'hello simple' )
                return commandReceiver
            }
            getName() >> 'simple'
        }
        shell.addService( command )

        and: 'mocked out streams'
        def ints = [ ]
        def out = new OutputStream() {
            @Override
            void write( int b ) { ints << b }
        }
        def err = Mock( OutputStream )

        when: 'the command is executed'
        shell.executePiped( [ new OsgiaasShell.Cmd( command, '' ) ] as LinkedList<OsgiaasShell.Cmd>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the OutputStream gets the text printed by the simple command'
        ints == 'hello simple\n'.bytes.collect { it as int }

        and: 'the command receiver gets no input but is closed'
        1 * commandReceiver.close()
        0 * _ // no more mock interactions are expected
    }

}
