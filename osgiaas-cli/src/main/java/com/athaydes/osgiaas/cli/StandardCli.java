package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.cli.Cli;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.api.service.DynamicServiceHelper;
import com.athaydes.osgiaas.api.service.HasManyServices;
import com.athaydes.osgiaas.cli.util.HasManyCommandCompleters;
import jline.console.completer.Completer;
import org.apache.felix.shell.Command;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class StandardCli extends HasManyServices<CommandModifier>
        implements Cli, CliProperties {

    private final AtomicReference<CliRun> currentRun = new AtomicReference<>();
    private final AtomicReference<Set<Command>> commands = new AtomicReference<>( new HashSet<>( 2 ) );
    private final OsgiaasShell shell = new OsgiaasShell( commands::get, this::getServices );

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
    protected Comparator<CommandModifier> getComparator() {
        return ( m1, m2 ) -> Integer.compare( m2.getPriority(), m1.getPriority() );
    }

    @Override
    public void start() {
        System.out.println( asciiArtLogo() );
        System.out.println( ":: https://github.com/renatoathaydes/osgiaas ::" );
        System.out.println();

        if ( currentRun.get() != null ) {
            System.out.println( "Already running!" );
            return;
        }

        try {
            Thread thread;

            synchronized ( currentRun ) {
                CliRun cli = new CliRun( shell::runCommand, this );
                thread = new Thread( cli );
                currentRun.set( cli );
            }

            thread.start();
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

    @Override
    public String[] availableCommands() {
        return shell.getCommands();
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

    public void addCommand( Command command ) {
        commands.updateAndGet( cmds -> {
            cmds.add( command );
            return new HashSet<>( cmds );
        } );

    }

    public void removeCommand( Command command ) {
        commands.updateAndGet( cmds -> {
            cmds.remove( command );
            return new HashSet<>( cmds );
        } );
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

    @Override
    public void clearScreen() {
        withCli( CliRun::clearScreen );
    }

    private void withCli( Consumer<CliRun> consumer ) {
        DynamicServiceHelper.with( currentRun, consumer );
    }

    private String asciiArtLogo() {
        return "   ____  _____ _______             _____    ________    ____\n" +
                "  / __ \\/ ___// ____(_)___ _____ _/ ___/   / ____/ /   /  _/\n" +
                " / / / /\\__ \\/ / __/ / __ `/ __ `/\\__ \\   / /   / /    / /  \n" +
                "/ /_/ /___/ / /_/ / / /_/ / /_/ /___/ /  / /___/ /____/ /   \n" +
                "\\____//____/\\____/_/\\__,_/\\__,_//____/   \\____/_____/___/   \n";
    }

}
