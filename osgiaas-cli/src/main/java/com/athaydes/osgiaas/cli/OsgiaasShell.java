package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.StreamingCommand;
import com.athaydes.osgiaas.api.stream.LineOutputStream;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * OSGiaaS Shell.
 */
public class OsgiaasShell {

    private final Supplier<Set<Command>> commandsProvider;

    public OsgiaasShell( Supplier<Set<Command>> commandsProvider ) {
        this.commandsProvider = commandsProvider;
    }

    public String[] getCommands() {
        Set<Command> services = commandsProvider.get();
        String[] result = new String[ services.size() ];
        int i = 0;
        for (Command cmd : services) {
            result[ i++ ] = cmd.getName();
        }
        return result;
    }

    public void executeCommand( String userCommand, PrintStream out, PrintStream err ) throws Exception {
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

        @Nullable
        Command cmd = findCommand( command );

        if ( cmd != null ) {
            if ( cmd instanceof StreamingCommand ) {
                out.println( "Piping " + cmd );
                StreamingCommand streamingCommand = ( StreamingCommand ) cmd;
                executePiped( new LinkedList<>(
                                Arrays.asList( new Cmd( streamingCommand, args ) ) ),
                        out, err );
            } else {
                cmd.execute( userCommand, out, err );
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

    @Nullable
    private Command findCommand( String name ) {
        for (Command cmd : commandsProvider.get()) {
            if ( cmd.getName().equals( name ) ) {
                return cmd;
            }
        }
        return null;
    }

    static class Cmd {
        final StreamingCommand cmd;
        final String args;

        public Cmd( StreamingCommand cmd, String args ) {
            this.cmd = cmd;
            this.args = args;
        }
    }

}
