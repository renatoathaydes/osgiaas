package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.ansi.Ansi;
import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.ansi.AnsiModifier;
import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.api.stream.NoOpPrintStream;
import com.athaydes.osgiaas.cli.util.InterruptableInputStream;
import com.athaydes.osgiaas.cli.util.OsgiaasPrintStream;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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
    private final InterruptableInputStream input;

    public CliRun( CommandRunner commandRunner,
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
            }
        } catch ( Exception e ) {
            System.err.println( "Unable to load osgiaas-cli history: " + e );
        } finally {
            showStatus( "" );
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
        input.interrupt();
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

        @Nullable FileHistory history = loadHistory();
        if ( history != null ) {
            consoleReader.setHistory( history );
        }

        runInitialCommands();

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
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            try {
                consoleReader.shutdown();
            } catch ( Exception e ) {
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

    public void clearScreen() {
        try {
            consoleReader.clearScreen();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
