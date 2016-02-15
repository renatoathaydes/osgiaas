package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.AnsiColor;
import com.athaydes.osgiaas.api.cli.Cli;
import com.athaydes.osgiaas.cli.util.DynamicServiceHelper;
import org.apache.felix.shell.ShellService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class StandardCli implements Cli {

    private final AtomicReference<CliRun> currentRun = new AtomicReference<>();
    private final AtomicReference<ShellService> shellService = new AtomicReference<>();

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
            CliRun cli = new CliRun( this::runCommand );
            Thread thread = new Thread( cli );
            currentRun.set( cli );
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

    private void runCommand( String command ) {
        withShellService( shell -> {
            try {
                shell.executeCommand( command, System.out, System.err );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }, () -> System.out.println( "Shell service is unavailable" ) );
    }

    @Override
    public void setPrompt( String prompt ) {
        withCli( cliRun -> cliRun.setPrompt( prompt ) );
    }

    @Override
    public void setPromptColor( AnsiColor color ) {
        withCli( cliRun -> cliRun.setPromptColor( color ) );
    }

    public void setShellService( ShellService shellService ) {
        this.shellService.set( shellService );
    }

    public void removeShellService( ShellService shellService ) {
        this.shellService.set( null );
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

//    public static void main( String[] args ) {
    //      new StandardCli().start();
    //}


}
