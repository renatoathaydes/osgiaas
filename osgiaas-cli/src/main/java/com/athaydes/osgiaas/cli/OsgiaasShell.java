package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.OsgiaasCommand;
import com.athaydes.osgiaas.api.stream.LineOutputStream;
import com.athaydes.osgiaas.cli.util.HasManyServices;
import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * OSGiaaS Shell.
 */
public class OsgiaasShell extends HasManyServices<Command> {

    public void execute( String userCommand, PrintStream out, PrintStream err ) {
        userCommand = userCommand.trim();
        int cmdLastIndex = userCommand.indexOf( ' ' );
        String command;
        String args;
        if ( cmdLastIndex < 0 ) {
            command = userCommand;
            args = "";
        } else {
            command = userCommand.substring( 0, cmdLastIndex );
            args = userCommand.substring( cmdLastIndex + 1, userCommand.length() );
        }
        Optional<Command> cmd = getServices().stream()
                .filter( it -> it.getName().equals( command ) )
                .findFirst();

        if ( cmd.isPresent() ) {
            if ( cmd.get() instanceof OsgiaasCommand ) {
                OsgiaasCommand osgiaasCommand = ( OsgiaasCommand ) cmd.get();
                executePiped( new LinkedList<>(
                                Arrays.asList( new Cmd( osgiaasCommand, args ) ) ),
                        out, err );
            }
        } else {
            err.println( "Command not found: " + command );
        }

    }

    void executePiped( LinkedList<Cmd> cmds, PrintStream out, PrintStream err ) {
        Consumer<String> lineConsumer = out::println;

        while ( !cmds.isEmpty() ) {
            Cmd current = cmds.removeLast();
            lineConsumer = current.cmd.pipe( current.args,
                    new PrintStream( new LineOutputStream( lineConsumer ), true ), err );
        }
    }

    static class Cmd {
        final OsgiaasCommand cmd;
        final String args;

        public Cmd( OsgiaasCommand cmd, String args ) {
            this.cmd = cmd;
            this.args = args;
        }
    }

}
