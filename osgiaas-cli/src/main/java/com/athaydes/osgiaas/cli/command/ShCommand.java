package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.StreamingCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ShCommand implements StreamingCommand {

    private final ExecutorService executorService = Executors.newFixedThreadPool( 2 );

    @Override
    public String getName() {
        return "sh";
    }

    @Override
    public String getUsage() {
        return "sh <shell command>";
    }

    @Override
    public String getShortDescription() {
        return "Runs a shell command.";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        String[] commands = line.trim().split( "\\s+" );
        if ( commands.length > 1 ) {
            commands = Arrays.copyOfRange( commands, 1, commands.length );
            runCommand( out, err, commands );
        } else {
            CommandHelper.printError( err, getUsage(), "No arguments provided" );
        }
    }

    private void runCommand( PrintStream out, PrintStream err, String[] commands ) {
        try {
            // TODO should keep a single session as long as possible to emulate a real shell session
            Process process = new ProcessBuilder( commands ).start();

            CountDownLatch latch = new CountDownLatch( 2 );

            consume( process.getInputStream(), out, err, latch );
            consume( process.getErrorStream(), err, err, latch );

            int exitValue = process.waitFor();
            boolean noTimeout = latch.await( 5, TimeUnit.SECONDS );

            if ( exitValue != 0 ) {
                err.println( "Process exit value: " + exitValue );
            }

            if ( !noTimeout ) {
                err.println( "Process timeout! Killing it forcefully" );
                process.destroyForcibly();
            }
        } catch ( IOException | InterruptedException e ) {
            e.printStackTrace( err );
        }
    }

    @Override
    public OutputStream pipe( String command, PrintStream out, PrintStream err ) {
        throw new UnsupportedOperationException( "pipe" );
    }

    private void consume( InputStream stream,
                          PrintStream writer,
                          PrintStream err,
                          CountDownLatch latch ) {
        executorService.submit( () -> {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader( stream ), 1024 );

            String nextLine;
            try {
                while ( ( nextLine = reader.readLine() ) != null ) {
                    writer.println( nextLine );
                }
            } catch ( Throwable e ) {
                e.printStackTrace( err );
            } finally {
                // done!
                latch.countDown();
            }
        } );
    }

}
