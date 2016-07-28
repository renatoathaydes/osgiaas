package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.cli.CommandHelper;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RunCommand implements Command {

    private final ExecutorService executorService = Executors.newFixedThreadPool( 2 );

    private Path workingDirectory = getUserHome();

    private static Path getUserHome() {
        @Nullable String home = System.getProperty( "user.home" );
        if ( home != null && new File( home ).isDirectory() ) {
            return Paths.get( home );
        } else {
            return Paths.get( "." );
        }
    }

    @Override
    public String getName() {
        return "run";
    }

    @Override
    public String getUsage() {
        return "run <program>";
    }

    @Override
    public String getShortDescription() {
        return "Runs a OS program.";
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void stop() {
        executorService.shutdownNow();
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        List<String> commands = CommandHelper.breakupArguments( line );
        if ( commands.size() > 1 ) {
            commands = commands.stream().skip( 1 ).collect( Collectors.toList() );
            runCommand( out, err, commands );
        } else {
            CommandHelper.printError( err, getUsage(), "No arguments provided" );
        }
    }

    private void runCommand( PrintStream out, PrintStream err, List<String> commands ) {
        // treat 'cd' command specially
        if ( commands.size() > 0 && commands.get( 0 ).equals( "cd" ) ) {
            if ( commands.size() > 2 ) {
                err.println( "Too many arguments" );
            } else {
                Path path = commands.size() == 1 ? getUserHome() : Paths.get( commands.get( 1 ) );
                changeDirectory( out, err, path );
            }
        } else try {
            Process process = new ProcessBuilder( commands )
                    .directory( workingDirectory.toFile() )
                    .redirectInput( ProcessBuilder.Redirect.INHERIT )
                    .start();

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

    private void changeDirectory( PrintStream out, PrintStream err, Path path ) {
        Path newDir = workingDirectory.resolve( path ).normalize();
        if ( newDir.toFile().isDirectory() ) {
            workingDirectory = newDir;
            out.println( workingDirectory );
        } else {
            err.println( newDir.toFile().exists() ?
                    "Not a directory" :
                    "Directory does not exist" );
        }
    }

    private void consume( InputStream stream,
                          PrintStream writer,
                          PrintStream err,
                          CountDownLatch latch ) {
        executorService.submit( () -> {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader( stream, StandardCharsets.UTF_8 ), 1024 );

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
