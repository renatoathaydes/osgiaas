package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.AnsiColor;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.cli.util.InterruptableInputStream;
import com.athaydes.osgiaas.cli.util.NoOpPrintStream;
import com.athaydes.osgiaas.cli.util.OsgiaasPrintStream;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
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

    public CliRun( CommandRunner commandRunner, CliProperties cliProperties )
            throws IOException {
        this.commandRunner = commandRunner;
        this.cliProperties = cliProperties;

        consoleReader = new ConsoleReader(
                new InterruptableInputStream( System.in ),
                System.out );

        // simple completer for the initial basic commands
        consoleReader.addCompleter( new StringsCompleter( cliProperties.availableCommands() ) );

        started = new AtomicBoolean( false );
        consoleReader.setPrompt( getPrompt() );

        loadHistory( consoleReader );
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
                Scanner fileScanner = new Scanner( initFile );
                while ( fileScanner.hasNextLine() ) {
                    PrintStream out = new NoOpPrintStream();
                    OsgiaasPrintStream err = new OsgiaasPrintStream(
                            System.err, cliProperties.getErrorColor() );

                    runCommand( fileScanner.nextLine(), out, err );
                }
            }
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

        runInitialCommands();

        thread = Thread.currentThread();

        try {
            String line;
            while ( ( line = consoleReader.readLine( getPrompt() ) ) != null ) {
                if ( !line.trim().isEmpty() ) {
                    runCommand( line );
                }
            }
            FileHistory history = ( FileHistory ) consoleReader.getHistory();
            history.flush();
            System.out.println( colored( "Bye!", AnsiColor.BLUE ) );
            consoleReader.shutdown();
        } catch ( Exception e ) {
            // only print stacktrace if the CLI was not interrupted by the user
            //noinspection ConstantConditions
            if ( !( e instanceof InterruptedException ) ) {
                e.printStackTrace();
            }
        }
    }

    private void runCommand( String line, PrintStream out, PrintStream err ) {
        commandRunner.runCommand( line, out, err );
    }

    private void runCommand( String line ) {
        OsgiaasPrintStream out = new OsgiaasPrintStream(
                System.out, cliProperties.getTextColor() );

        OsgiaasPrintStream err = new OsgiaasPrintStream(
                System.err, cliProperties.getErrorColor() );

        runCommand( line, out, err );
    }

    static String colored( String text, AnsiColor color ) {
        return color + text + AnsiColor.RESET;
    }

}
