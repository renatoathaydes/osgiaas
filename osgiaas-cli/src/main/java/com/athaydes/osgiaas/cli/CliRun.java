package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.ansi.Ansi;
import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.cli.util.InterruptableInputStream;
import com.athaydes.osgiaas.cli.util.NoOpPrintStream;
import com.athaydes.osgiaas.cli.util.OsgiaasPrintStream;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
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

    public CliRun( CommandRunner commandRunner,
                   CliProperties cliProperties )
            throws IOException {
        this.commandRunner = commandRunner;
        this.cliProperties = cliProperties;

        consoleReader = new ConsoleReader(
                new InterruptableInputStream( System.in ),
                System.out );

        started = new AtomicBoolean( false );
        consoleReader.setPrompt( getPrompt() );
    }

    void addCompleter( Completer completer ) {
        consoleReader.addCompleter( completer );
    }

    void removeCompleter( Completer completer ) {
        consoleReader.removeCompleter( completer );
    }

    @Nullable
    private static FileHistory loadHistory() {
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
            return new FileHistory( historyFile );
        } catch ( Exception e ) {
            System.err.println( "Unable to load osgiaas-cli history: " + e );
            return null;
        }
    }

    private void runInitialCommands() {
        try {
            File userHome = new File( System.getProperty( "user.home", "." ) );
            @Nullable
            String initFileLocation = System.getProperty( "osgiaas.cli.init" );
            File initFile;
            if ( initFileLocation == null ) {
                initFile = new File( userHome, ".osgiaas_cli_init" );
            } else {
                initFile = new File( initFileLocation );
            }
            if ( initFile.exists() ) {
                showStatus( "Running init commands" );
                Thread.sleep( 250L ); // allows commands to be loaded

                Scanner fileScanner = new Scanner( initFile );

                PrintStream out = new NoOpPrintStream();
                OsgiaasPrintStream err = new OsgiaasPrintStream(
                        System.err, cliProperties.getErrorColor() );

                int index = 1;

                while ( fileScanner.hasNextLine() ) {
                    showStatus( "Running init commands [" + index + "]" );
                    commandRunner.runCommand( fileScanner.nextLine(), out, err );
                    index++;
                }

                showStatus( "" );
            }
        } catch ( Exception e ) {
            System.err.println( "Unable to load osgiaas-cli history: " + e );
        }

    }

    private void showStatus( String status ) {
        try {
            consoleReader.resetPromptLine( "", status, -1 );
        } catch ( IOException e ) {
            e.printStackTrace();
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
        return Ansi.applyColor(
                cliProperties.getPrompt(),
                cliProperties.getPromptColor() ) + AnsiColor.RESET;
    }

    @Override
    public void run() {
        if ( started.getAndSet( true ) ) {
            // already started
            return;
        }

        @Nullable FileHistory history = loadHistory();
        if ( history != null ) {
            consoleReader.setHistory( history );
        }

        runInitialCommands();

        thread = Thread.currentThread();

        try {
            String line;
            while ( ( line = consoleReader.readLine( getPrompt() ) ) != null ) {
                if ( !line.trim().isEmpty() ) {
                    try {
                        runCommand( line );
                    } catch ( Throwable e ) {
                        e.printStackTrace();
                    }
                }
            }

            if ( history != null ) {
                history.flush();
            }

            System.out.println( Ansi.applyColor( "Bye!", AnsiColor.BLUE ) );
            consoleReader.shutdown();
        } catch ( Exception e ) {
            // only print stacktrace if the CLI was not interrupted by the user
            //noinspection ConstantConditions
            if ( !( e instanceof InterruptedException ) ) {
                e.printStackTrace();
            }
        }
    }

    private void runCommand( String line ) {
        OsgiaasPrintStream out = new OsgiaasPrintStream(
                System.out, cliProperties.getTextColor() );

        OsgiaasPrintStream err = new OsgiaasPrintStream(
                System.err, cliProperties.getErrorColor() );

        commandRunner.runCommand( line, out, err );
    }

}
