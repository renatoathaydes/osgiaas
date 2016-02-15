package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.AnsiColor;
import com.athaydes.osgiaas.cli.util.InterruptableInputStream;
import jline.console.ConsoleReader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a single run of the CLI.
 * <p>
 * It allows the management of a CLI session without worrying about threading issues.
 */
public class CliRun implements Runnable {

    private volatile String prompt = ">> ";
    private volatile AnsiColor promptColor = AnsiColor.RED;

    private final ConsoleReader consoleReader;
    private final AtomicBoolean started;
    private final CommandRunner commandRunner;

    @Nullable
    private volatile Thread thread = null;

    public CliRun( CommandRunner commandRunner ) throws IOException {
        this.commandRunner = commandRunner;
        consoleReader = new ConsoleReader( new InterruptableInputStream( System.in ), System.out );
        started = new AtomicBoolean( false );
        resetPrompt();
    }

    public void stop() {
        @Nullable
        Thread currentThread = thread;
        if ( currentThread != null ) {
            currentThread.interrupt();
        }
    }

    public void setPrompt( String prompt ) {
        this.prompt = prompt;
        resetPrompt();
    }

    public void setPromptColor( AnsiColor color ) {
        this.promptColor = color;
        resetPrompt();
    }

    private void resetPrompt() {
        consoleReader.setPrompt( colored( prompt, promptColor ) );
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
            while ( ( line = consoleReader.readLine() ) != null ) {
                commandRunner.runCommand( line, System.out, System.err );
            }
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
