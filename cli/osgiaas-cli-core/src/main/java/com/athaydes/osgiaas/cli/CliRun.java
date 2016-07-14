package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.ansi.Ansi;
import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.ansi.AnsiModifier;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.cli.util.InterruptableInputStream;
import com.athaydes.osgiaas.cli.util.OsgiaasPrintStream;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a single run of the CLI service.
 * <p>
 * It allows the management of a CLI session without worrying about threading issues.
 */
class CliRun implements Runnable {

    private final ConsoleReader consoleReader;
    private final AtomicBoolean started;
    private final CommandRunner commandRunner;
    private final CliProperties cliProperties;
    private final InterruptableInputStream input;
    private final CountDownLatch killWaiter = new CountDownLatch( 1 );

    CliRun( CommandRunner commandRunner,
            CliProperties cliProperties )
            throws IOException {
        this.commandRunner = commandRunner;
        this.cliProperties = cliProperties;
        this.input = new InterruptableInputStream( System.in );

        consoleReader = new ConsoleReader( input, System.out );
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

    private void showStatus( String status ) {
        try {
            consoleReader.resetPromptLine( "", status, -1 );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    void stop() {
        input.interrupt();
        try {
            killWaiter.await( 500, TimeUnit.MILLISECONDS );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    }

    private String getPrompt() {
        AnsiColor promptColor = cliProperties.getPromptColor();
        String commandBeingUsed = cliProperties.commandBeingUsed();

        String using = "";
        if ( !commandBeingUsed.isEmpty() ) {
            using = Ansi.applyAnsi( "[using " + commandBeingUsed.trim() + "]\n",
                    new AnsiColor[]{ promptColor },
                    AnsiModifier.ITALIC, AnsiModifier.HIGH_INTENSITY ) + AnsiColor.RESET;
        }

        return using + Ansi.applyColor(
                cliProperties.getPrompt(),
                promptColor ) + AnsiColor.RESET;
    }

    @Override
    public void run() {
        if ( started.getAndSet( true ) ) {
            // already started
            return;
        }

        InitCommandsRunner.scheduleInitCommands( cliProperties, commandRunner, this::showStatus );

        @Nullable FileHistory history = loadHistory();
        if ( history != null ) {
            consoleReader.setHistory( history );
        }

        try {
            List<String> lines = new ArrayList<>( 2 );
            boolean multiline = false;
            String line;
            while ( ( line = consoleReader.readLine( multiline ? "" : getPrompt() ) ) != null ) {
                String trimmedLine = line.trim();
                final boolean execute;
                if ( !multiline && trimmedLine.equals( ":{" ) ) {
                    multiline = true;
                    execute = false;
                } else if ( multiline && trimmedLine.equals( ":}" ) ) {
                    multiline = false;
                    execute = true;
                    line = "";
                } else if ( multiline ) {
                    lines.add( line );
                    execute = false;
                } else {
                    execute = true;
                }

                if ( execute && ( !trimmedLine.isEmpty() || !lines.isEmpty() ) ) {
                    lines.add( line );
                    try {
                        runCommand( String.join( "\n", lines ) );
                    } catch ( Throwable e ) {
                        e.printStackTrace();
                    } finally {
                        lines = new ArrayList<>( 2 );
                    }
                }
            }

            if ( history != null ) {
                history.flush();
            }

            System.out.println( Ansi.applyColor( "Bye!", AnsiColor.BLUE ) );
        } catch ( Throwable e ) {
            e.printStackTrace();
        } finally {
            try {
                consoleReader.shutdown();
            } catch ( Throwable e ) {
                e.printStackTrace();
            }

            killWaiter.countDown();
        }
    }

    private void runCommand( String line ) {
        OsgiaasPrintStream out = new OsgiaasPrintStream(
                System.out, cliProperties.getTextColor() );

        OsgiaasPrintStream err = new OsgiaasPrintStream(
                System.err, cliProperties.getErrorColor() );

        commandRunner.runCommand( line, out, err );
    }

    void clearScreen() {
        try {
            consoleReader.clearScreen();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
