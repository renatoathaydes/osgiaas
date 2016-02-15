package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.AnsiColor;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.cli.util.InterruptableInputStream;
import com.athaydes.osgiaas.cli.util.OsgiaasPrintStream;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a single run of the CLI service.
 * <p>
 * It allows the management of a CLI session without worrying about threading issues.
 */
public class CliRun implements Runnable {

    private final ConsoleReader consoleReader;
    private final AtomicBoolean started;
    private final CommandRunner commandRunner;
    private final CliProperties cliProperties;

    @Nullable
    private volatile Thread thread = null;

    public CliRun( CommandRunner commandRunner, CliProperties cliProperties )
            throws IOException {
        this.commandRunner = commandRunner;
        this.cliProperties = cliProperties;

        consoleReader = new ConsoleReader(
                new InterruptableInputStream( System.in ),
                System.out );

        loadHistory( consoleReader );

        started = new AtomicBoolean( false );

        consoleReader.setPrompt( getPrompt() );
    }

    private static void loadHistory( ConsoleReader consoleReader ) {
        try {
            File userHome = new File( System.getProperty( "user.home", "." ) );
            @Nullable
            String historyFileLocation = System.getProperty( "osgiaas.cli.history" );
            File historyFile;
            if ( historyFileLocation == null ) {
                historyFile = new File( userHome, ".osgiaas_cli_history" );
            } else {
                historyFile = new File( historyFileLocation );
            }
            consoleReader.setHistory( new FileHistory( historyFile ) );
        } catch ( Exception e ) {
            System.err.println( "Unable to load osgiaas-cli history: " + e );
        }
    }

    public void stop() {
        @Nullable
        Thread currentThread = thread;
        if ( currentThread != null ) {
            currentThread.interrupt();
        }
    }

    private String getPrompt() {
        return colored(
                cliProperties.getPrompt(),
                cliProperties.getPromptColor() );
    }

    @Override
    public void run() {
        if ( started.getAndSet( true ) ) {
            // already started
            return;
        }

        thread = Thread.currentThread();

        try {
            String line;
            while ( ( line = consoleReader.readLine( getPrompt() ) ) != null ) {
                OsgiaasPrintStream out = new OsgiaasPrintStream(
                        System.out, cliProperties.getTextColor() );

                OsgiaasPrintStream err = new OsgiaasPrintStream(
                        System.err, cliProperties.getErrorColor() );

                commandRunner.runCommand( line, out, err );
            }
            FileHistory history = ( FileHistory ) consoleReader.getHistory();
            history.flush();
            System.out.println( colored( "Bye!", AnsiColor.BLUE ) );
            consoleReader.shutdown();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    static String colored( String text, AnsiColor color ) {
        return color + text + AnsiColor.RESET;
    }

}
