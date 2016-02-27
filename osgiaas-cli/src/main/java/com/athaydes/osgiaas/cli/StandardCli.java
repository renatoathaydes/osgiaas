package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.AnsiColor;
import com.athaydes.osgiaas.api.cli.Cli;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.cli.util.DynamicServiceHelper;
import com.athaydes.osgiaas.cli.util.HasManyCommandCompleters;
import com.athaydes.osgiaas.cli.util.HasManyServices;
import jline.console.completer.Completer;
import org.apache.felix.shell.ShellService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class StandardCli extends HasManyServices<CommandModifier>
        implements Cli, CliProperties {

    private final AtomicReference<CliRun> currentRun = new AtomicReference<>();
    private final AtomicReference<ShellService> shellService = new AtomicReference<>();

    private final HasManyCommandCompleters completers = new HasManyCommandCompleters() {
        @Override
        protected void addCompleter( Completer completer ) {
            withCli( cli -> cli.addCompleter( completer ) );
        }

        @Override
        protected void removeCompleter( Completer completer ) {
            withCli( cli -> cli.removeCompleter( completer ) );
        }
    };

    private volatile String prompt = ">> ";
    private volatile AnsiColor promptColor = AnsiColor.RESET;
    private volatile AnsiColor textColor = AnsiColor.RESET;
    private volatile AnsiColor errorColor = AnsiColor.RED;

    @Override
    public void start() {
        System.out.println( asciiArtLogo() );
        System.out.println( ":: cli.athaydes.com ::" );
        System.out.println();

        if ( currentRun.get() != null ) {
            System.out.println( "Already running!" );
            return;
        }

        try {
            synchronized (currentRun) {
                CliRun cli = new CliRun( this::runCommand, this, completers.getCompleters() );
                Thread thread = new Thread( cli );
                currentRun.set( cli );
                thread.start();
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.out.println( "Stopping StandardCli" );
        withCli( cli -> {
            cli.stop();
            currentRun.set( null );
        } );
    }

    private void runCommand( String command, PrintStream out, PrintStream err ) {
        runCommand( command, out, err, "" );
    }

    private void runCommand( String command, PrintStream out, PrintStream err, String argument ) {
        withShellService( shell -> {
            try {
                List<String> transformedCommands = transformCommand( command.trim(), getServices() );
                for (String cmd : transformedCommands) {
                    if ( cmd.contains( "|" ) ) {
                        runWithPipes( cmd, out, err );
                    } else {
                        shell.executeCommand( cmd + " " + argument, out, err );
                    }
                }
            } catch ( Exception e ) {
                e.printStackTrace( err );
            }
        }, () -> err.println( "Shell service is unavailable" ) );
    }

    private void runWithPipes( String command, PrintStream out, PrintStream err )
            throws Exception {
        String[] parts = command.split( "\\|" );

        if ( parts.length <= 1 ) {
            throw new RuntimeException( "runWithPipes called without pipe" );
        } else {
            String prevOutput = "";
            int index = parts.length;

            for (String currCmd : parts) {
                index--;

                boolean lastItem = index == 0;

                if ( lastItem ) {
                    runCommand( currCmd, out, err, prevOutput );
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

    @Override
    public String[] availableCommands() {
        AtomicReference<String[]> result = new AtomicReference<>( new String[ 0 ] );
        withShellService( shell -> result.set( shell.getCommands() ), () ->
                System.out.println( "Shell service is unavailable" ) );
        return result.get();
    }

    @Override
    public void setPrompt( String prompt ) {
        this.prompt = prompt;
    }

    @Override
    public void setPromptColor( AnsiColor color ) {
        promptColor = color;
    }

    @Override
    public void setErrorColor( AnsiColor color ) {
        errorColor = color;
    }

    @Override
    public void setTextColor( AnsiColor color ) {
        textColor = color;
    }

    public void setShellService( ShellService shellService ) {
        this.shellService.set( shellService );
    }

    public void removeShellService( ShellService shellService ) {
        this.shellService.set( null );
    }

    public void addCommandModifier( CommandModifier commandModifier ) {
        addService( commandModifier );
    }

    public void removeCommandModifier( CommandModifier commandModifier ) {
        removeService( commandModifier );
    }

    public void addCommandCompleter( CommandCompleter commandCompleter ) {
        completers.addService( commandCompleter );
    }

    public void removeCommandCompleter( CommandCompleter commandCompleter ) {
        completers.removeService( commandCompleter );
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    @Override
    public AnsiColor getPromptColor() {
        return promptColor;
    }

    @Override
    public AnsiColor getTextColor() {
        return textColor;
    }

    @Override
    public AnsiColor getErrorColor() {
        return errorColor;
    }

    private void withCli( Consumer<CliRun> consumer ) {
        DynamicServiceHelper.with( currentRun, consumer );
    }

    private void withShellService( Consumer<ShellService> consumer, Runnable onUnavailable ) {
        DynamicServiceHelper.with( shellService, consumer, onUnavailable );
    }

    private String asciiArtLogo() {
        return "   ____  _____ _______             _____    ________    ____\n" +
                "  / __ \\/ ___// ____(_)___ _____ _/ ___/   / ____/ /   /  _/\n" +
                " / / / /\\__ \\/ / __/ / __ `/ __ `/\\__ \\   / /   / /    / /  \n" +
                "/ /_/ /___/ / /_/ / / /_/ / /_/ /___/ /  / /___/ /____/ /   \n" +
                "\\____//____/\\____/_/\\__,_/\\__,_//____/   \\____/_____/___/   \n";
    }

}
