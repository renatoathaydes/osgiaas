package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.api.cli.StreamingCommand;
import com.athaydes.osgiaas.api.stream.LineOutputStream;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OSGiaaS Shell.
 */
public class OsgiaasShell {

    private final Supplier<Set<Command>> commandsProvider;
    private final Supplier<Set<CommandModifier>> modifiersProvider;

    public OsgiaasShell( Supplier<Set<Command>> commandsProvider,
                         Supplier<Set<CommandModifier>> modifiersProvider ) {
        this.commandsProvider = commandsProvider;
        this.modifiersProvider = modifiersProvider;
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

    public void executeCommand( String userCommand, PrintStream out, PrintStream err ) {
        userCommand = userCommand.trim();
        int cmdLastIndex = userCommand.indexOf( ' ' );
        String command;
        if ( cmdLastIndex < 0 ) {
            command = userCommand;
        } else {
            command = userCommand.substring( 0, cmdLastIndex );
        }

        @Nullable
        Command cmd = findCommand( command );

        if ( cmd != null ) {
            if ( cmd instanceof StreamingCommand ) {
                StreamingCommand streamingCommand = ( StreamingCommand ) cmd;
                executePiped( new LinkedList<>(
                                Arrays.asList( new Cmd( streamingCommand, userCommand ) ) ),
                        out, err );
            } else {
                cmd.execute( userCommand, out, err );
            }
        } else {
            err.println( "Command not found: " + command );
        }

    }

    public void runCommand( String command, PrintStream out, PrintStream err ) {
        runCommand( command, out, err, "" );
    }

    private void runCommand( String command, PrintStream out, PrintStream err, String argument ) {
        try {
            List<String> transformedCommands = transformCommand( command.trim(), modifiersProvider.get() );
            for (String cmd : transformedCommands) {
                if ( cmd.contains( "|" ) ) {
                    runWithPipes( cmd, out, err );
                } else {
                    executeCommand( cmd + " " + argument, out, err );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace( err );
        }
    }

    static List<String> transformCommand( String command, Collection<CommandModifier> modifiers ) {
        List<String> nextCommands = new ArrayList<>();
        for (CommandModifier modifier : modifiers) {
            List<String> commands = new ArrayList<>( modifier.apply( command ) );
            if ( commands.isEmpty() ) {
                return commands;
            }
            command = commands.remove( 0 );
            nextCommands.addAll( commands );
        }

        if ( nextCommands.isEmpty() ) {
            return Collections.singletonList( command );
        } else {
            List<String> result = new ArrayList<>();
            result.add( command );
            for (String cmd : nextCommands) {
                result.addAll( transformCommand( cmd, modifiers ) );
            }
            return result;
        }
    }

    void executePiped( LinkedList<Cmd> cmds, PrintStream out, PrintStream err ) {
        Consumer<String> lineConsumer = out::println;

        while ( !cmds.isEmpty() ) {
            Cmd current = cmds.removeLast();
            lineConsumer = current.cmd.pipe( current.userCommand,
                    new PrintStream( new LineOutputStream( lineConsumer ), true ), err );
        }
    }

    private void runWithPipes( String command, PrintStream out, PrintStream err )
            throws Exception {
        String[] parts = command.split( "\\|" );

        if ( parts.length <= 1 ) {
            throw new RuntimeException( "runWithPipes called without pipe" );
        } else {
            String prevOutput = "";
            int index = parts.length;

            final Pattern specialPipePattern = Pattern.compile( ">([A-z_]*)\\s+.*" );

            for (String currCmd : parts) {
                index--;

                Matcher specialPipe = specialPipePattern.matcher( currCmd );

                @Nullable
                String specialPipeVariable;

                if ( specialPipe.matches() &&
                        ( specialPipeVariable = specialPipe.group( 1 ) ) != null ) {
                    if ( specialPipeVariable.isEmpty() ) {
                        currCmd = currCmd.substring( 1 );
                        specialPipeVariable = "it";
                    } else {
                        currCmd = currCmd.substring( specialPipeVariable.length() + 1 );
                    }
                } else {
                    specialPipeVariable = null;
                }

                boolean lastItem = index == 0;

                if ( lastItem ) {
                    if ( specialPipeVariable != null ) {
                        String cmd = currCmd.replaceAll(
                                Pattern.quote( "$(" + specialPipeVariable + ")" ),
                                prevOutput );
                        runCommand( cmd, out, err );
                    } else {
                        runCommand( currCmd, out, err, prevOutput );
                    }
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream( 1024 );
                    try ( PrintStream currOut = new PrintStream( baos, true, "UTF-8" ) ) {
                        runCommand( currCmd, currOut, err, prevOutput );
                    }
                    prevOutput = baos.toString( "UTF-8" );
                }
            }
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
        final String userCommand;

        public Cmd( StreamingCommand cmd, String args ) {
            this.cmd = cmd;
            this.userCommand = args;
        }
    }

}
