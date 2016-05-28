package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.api.cli.StreamingCommand;
import com.athaydes.osgiaas.api.stream.LineAccumulatorOutputStream;
import com.athaydes.osgiaas.api.stream.LineOutputStream;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * OSGiaaS Shell.
 */
public class OsgiaasShell {

    private final Supplier<Set<Command>> commandsProvider;
    private final Supplier<List<CommandModifier>> modifiersProvider;

    private static final Function<String, List<String>> breakUpPipes = ( line ) -> {
        List<String> result = new ArrayList<>( 2 );
        CommandHelper.breakupArguments( line,
                result::add,
                CommandHelper.CommandBreakupOptions.create()
                        .includeQuotes( true )
                        .separatorCode( '|' ) );
        return result;
    };

    public OsgiaasShell( Supplier<Set<Command>> commandsProvider,
                         Supplier<List<CommandModifier>> modifiersProvider ) {
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

    public void runCommand( String userCommand, PrintStream out, PrintStream err ) {
        List<CommandModifier> commandModifiers = modifiersProvider.get();
        LinkedList<List<Cmd>> commandsPipeline = new LinkedList<>();
        List<String> pipes = breakUpPipes.apply( userCommand );

        try {
            for (String pipe : pipes) {
                List<String> transformedCommands = transformCommand( pipe.trim(), commandModifiers );
                List<Cmd> commands = new ArrayList<>( transformedCommands.size() );

                for (String command : transformedCommands) {
                    @Nullable Command actualCommand = tryGetCommand( command, err );
                    if ( actualCommand == null ) {
                        return;
                    }
                    commands.add( new Cmd( actualCommand, command ) );
                }

                commandsPipeline.add( commands );
            }

            if ( commandsPipeline.size() > 1 ) {
                executePiped( commandsPipeline, out, err );
            } else if ( commandsPipeline.size() == 1 ) {
                List<Cmd> commandInvocations = commandsPipeline.get( 0 );
                for (Cmd command : commandInvocations) {
                    command.cmd.execute( command.userCommand, out, err );
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

    void executePiped( LinkedList<List<Cmd>> pipeline, PrintStream out, PrintStream err ) throws Exception {
        OutputStream lineConsumer = new LineOutputStream( out::println, () -> {
        } );

        while ( !pipeline.isEmpty() ) {
            List<Cmd> currentCmds = pipeline.removeLast();
            if ( currentCmds.isEmpty() ) {
                continue;
            }

            Cmd current = executeAllButLast( currentCmds, out, err );
            boolean firstCommand = pipeline.isEmpty();
            Command cmd = current.cmd;

            if ( !firstCommand && cmd instanceof StreamingCommand ) {
                lineConsumer = ( ( StreamingCommand ) cmd ).pipe( current.userCommand,
                        new PrintStream( lineConsumer, true ), err );
            } else {
                final OutputStream nextLineConsumer = lineConsumer;
                lineConsumer = new LineAccumulatorOutputStream( ( allLines ) ->
                        cmd.execute( current.userCommand + " " + allLines,
                                new PrintStream( nextLineConsumer, true ), err )
                        , nextLineConsumer );
            }
        }

        // the first consumer in the pipeline is closed without further input
        lineConsumer.close();
    }

    private Cmd executeAllButLast( List<Cmd> currentCmds, PrintStream out, PrintStream err ) {
        for (int i = 0; i < currentCmds.size() - 1; i++) {
            Cmd command = currentCmds.get( i );
            command.cmd.execute( command.userCommand, out, err );
        }
        return currentCmds.get( currentCmds.size() - 1 );
    }

    private Command tryGetCommand( String userCommand, PrintStream err ) {
        String commandName = extractCommandNameFrom( userCommand );
        @Nullable Command cmd = findCommand( commandName );
        if ( cmd == null ) {
            err.println( "Command not found: " + commandName );
            return null;
        } else {
            return cmd;
        }
    }

    private static String extractCommandNameFrom( String userCommand ) {
        userCommand = userCommand.trim();
        int cmdLastIndex = userCommand.indexOf( ' ' );

        if ( cmdLastIndex < 0 ) {
            return userCommand;
        } else {
            return userCommand.substring( 0, cmdLastIndex );
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
        final Command cmd;
        final String userCommand;

        public Cmd( Command cmd, String userCommand ) {
            this.cmd = cmd;
            this.userCommand = userCommand;
        }
    }

}
