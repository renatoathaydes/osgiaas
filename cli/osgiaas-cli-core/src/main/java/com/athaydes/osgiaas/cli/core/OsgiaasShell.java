package com.athaydes.osgiaas.cli.core;

import com.athaydes.osgiaas.api.stream.LineOutputStream;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandModifier;
import com.athaydes.osgiaas.cli.StreamingCommand;
import com.athaydes.osgiaas.cli.core.util.LineAccumulatorOutputStream;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * OSGiaaS Shell.
 */
public class OsgiaasShell implements CommandRunner {

    private final Commands commands;
    private final Supplier<List<CommandModifier>> modifiersProvider;


    private static final CommandHelper.CommandBreakupOptions pipesBreakupOptions =
            CommandHelper.CommandBreakupOptions.create()
                    .includeQuotes( true )
                    .separatorCode( '|' );

    public OsgiaasShell( Commands commands,
                         Supplier<List<CommandModifier>> modifiersProvider ) {
        this.commands = commands;
        this.modifiersProvider = modifiersProvider;
    }

    public String[] getCommands() {
        Set<String> commandNames = commands.getCommandNames();
        return commandNames.toArray( new String[ commandNames.size() ] );
    }

    @Override
    public void runWhenAvailable( String userCommand, PrintStream out,
                                  PrintStream err, Duration timeout ) {
        String commandName = extractCommandNameFrom( userCommand );
        commands.runNowOrLater( commandName,
                ( cmd ) -> runCommand( userCommand, out, err ),
                timeout );
    }

    @Override
    public void runCommand( String userCommand, PrintStream out, PrintStream err ) {
        List<CommandModifier> commandModifiers = modifiersProvider.get();
        LinkedList<List<Cmd>> commandsPipeline = new LinkedList<>();
        List<String> pipes = breakupPipes( userCommand );

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

    private static List<String> breakupPipes( String line ) {
        List<String> result = new ArrayList<>( 2 );
        CommandHelper.breakupArguments( line, result::add, pipesBreakupOptions );
        return result;
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

            PrintStream cmdOut = new PrintStream( lineConsumer, true );

            Cmd current = executeAllButLast( currentCmds, cmdOut, err );
            boolean firstCommand = pipeline.isEmpty();
            Command cmd = current.cmd;


            if ( !firstCommand && cmd instanceof StreamingCommand ) {
                lineConsumer = ( ( StreamingCommand ) cmd )
                        .pipe( current.userCommand, cmdOut, err );
            } else {
                lineConsumer = new LineAccumulatorOutputStream( ( allLines ) ->
                        cmd.execute( current.userCommand + " " + allLines, cmdOut, err )
                        , lineConsumer );
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
        @Nullable Command cmd = commands.getCommand( commandName );
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

    static class Cmd {
        final Command cmd;
        final String userCommand;

        public Cmd( Command cmd, String userCommand ) {
            this.cmd = cmd;
            this.userCommand = userCommand;
        }
    }

}
