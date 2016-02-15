package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.AnsiColor;
import com.athaydes.osgiaas.api.cli.Cli;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.cli.util.DynamicServiceHelper;
import org.apache.felix.shell.ShellService;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class StandardCli implements Cli, CliProperties {

    private final AtomicReference<CliRun> currentRun = new AtomicReference<>();
    private final AtomicReference<ShellService> shellService = new AtomicReference<>();

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
            CliRun cli = new CliRun( this::runCommand, this );
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

    private void runCommand( String command, PrintStream out, PrintStream err ) {
        withShellService( shell -> {
            try {
                shell.executeCommand( command, out, err );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }, () -> System.out.println( "Shell service is unavailable" ) );
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

//    public static void main( String[] args ) {
    //      new StandardCli().start();
    //}


}
