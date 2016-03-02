package com.athaydes.osgiaas.cli

import com.athaydes.osgiaas.api.cli.OsgiaasCommand
import spock.lang.Specification

@SuppressWarnings( "GroovyAssignabilityCheck" )
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
        def output = [ ]
        def out = outputStreamWith( output )
        def err = Mock( OutputStream )

        when: 'the command is executed'
        shell.executePiped( [ new OsgiaasShell.Cmd( command, '' ) ] as LinkedList<OsgiaasShell.Cmd>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the OutputStream gets the text printed by the simple command'
        output == 'hello simple\n'.bytes.collect { it as int }

        and: 'the command receiver gets no input but is closed'
        1 * commandReceiver.close()
    }

    def "OSGiaaS Shell can execute piped commands"() {
        given: 'a few simple commands are added to the shell'
        def receiver1 = [ ]
        def commandReceiver1 = outputStreamWith( receiver1 )
        def command1 = Stub( OsgiaasCommand ) {
            pipe( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                out.println( 'c1' )
                return commandReceiver1
            }
            getName() >> 'c1'
        }

        def receiver2 = [ ]
        def commandReceiver2 = outputStreamWith( receiver2 )
        def command2 = Stub( OsgiaasCommand ) {
            pipe( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                out.println( 'c2' )
                return commandReceiver2
            }
            getName() >> 'c2'
        }

        def receiver3 = [ ]
        def commandReceiver3 = outputStreamWith( receiver3 )
        def command3 = Stub( OsgiaasCommand ) {
            pipe( _, _, _ ) >> { String line, PrintStream out, PrintStream err ->
                out.println( 'c3' )
                return commandReceiver3
            }
            getName() >> 'c3'
        }

        shell.addService( command1 )
        shell.addService( command2 )
        shell.addService( command3 )

        and: 'mocked out streams'
        def output = [ ]
        def out = outputStreamWith( output )
        def err = Mock( OutputStream )

        when: 'the commands are executed piped'
        shell.executePiped( [
                new OsgiaasShell.Cmd( command1, '' ),
                new OsgiaasShell.Cmd( command2, '' ),
                new OsgiaasShell.Cmd( command3, '' )
        ] as LinkedList<OsgiaasShell.Cmd>,
                new PrintStream( out ), new PrintStream( err ) )

        then: 'the OutputStream gets the text printed by the last command'
        output == 'c3\n'.bytes.collect { it as int }

        and: 'the command receivers for the second and third commands get the expected input'
        receiver1 == [ ]
        receiver2 == 'c1\n'.bytes.collect { it as int }
        receiver3 == 'c2\n'.bytes.collect { it as int }

        and: 'only the first command receiver is closed (the others may be the actual user out and should not be closed)'
        1 * commandReceiver1.close()
        0 * commandReceiver2.close()
        0 * commandReceiver3.close()
    }

    private OutputStream outputStreamWith( List collector ) {
        def delegate = new OutputStream() {
            @Override
            void write( int b ) {
                collector << b
            }
        }
        Mock( OutputStream ) {
            write( _ ) >> { int b -> delegate.write( b ) }
            write( _, _, _ ) >> { byte[] bs, int off, int len ->
                delegate.write( bs, off, len )
            }
        }
    }

}
