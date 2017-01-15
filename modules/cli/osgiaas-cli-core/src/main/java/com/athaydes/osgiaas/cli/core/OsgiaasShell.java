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

                for (String commandInvocation : transformedCommands) {
                    @Nullable Command actualCommand = tryGetCommand( commandInvocation, err );
                    if ( actualCommand == null ) {
                        return;
                    }
                    commands.add( new Cmd( actualCommand, commandInvocation ) );
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
        // we initially assign lineConsumer to an instance that will print to the console and never closes its stream
        OutputStream lineConsumer = new LineOutputStream( out::println, () -> {
        } );

        while ( !pipeline.isEmpty() ) {
            List<Cmd> serialCmds = pipeline.removeLast();
            if ( serialCmds.isEmpty() ) {
                continue;
            }

            // the output of the current command-series is the lineConsumer of the next one
            // (we're iterating from last to first)
            PrintStream cmdOut = new PrintStream( lineConsumer, true, "UTF-8" );

            Cmd lastCmd = executeAllButLast( serialCmds, cmdOut, err );
            Command cmd = lastCmd.cmd;

            boolean isFirstCommand = pipeline.isEmpty();

            // if this command does not support streaming, or is the first command,
            // we need to block the pipeline on it
            if ( !isFirstCommand && cmd instanceof StreamingCommand ) {
                lineConsumer = new LineOutputStream( ( ( StreamingCommand ) cmd )
                        .pipe( lastCmd.userCommand, cmdOut, err ), cmdOut );
            } else {
                lineConsumer = new LineAccumulatorOutputStream( ( allLines ) ->
                        cmd.execute( lastCmd.userCommand + " " + allLines, cmdOut, err )
                        , lineConsumer );
            }
        }

        // the first consumer in the pipeline is closed without further input
        lineConsumer.close();
    }

    private Cmd executeAllButLast( List<Cmd> currentCmds, PrintStream out, PrintStream err ) {
        // serial commands must execute blocking
        for (int i = 0; i < currentCmds.size() - 1; i++) {
            Cmd command = currentCmds.get( i );
            command.cmd.execute( command.userCommand, out, err );
        }
        return currentCmds.get( currentCmds.size() - 1 );
    }

    @Nullable
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

        Cmd( Command cmd, String userCommand ) {
            this.cmd = cmd;
            this.userCommand = userCommand;
        }
    }

}
