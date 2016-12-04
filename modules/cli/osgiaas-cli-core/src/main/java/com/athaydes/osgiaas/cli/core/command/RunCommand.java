package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.api.env.NativeCommandRunner;
import com.athaydes.osgiaas.cli.CommandHelper;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class RunCommand implements Command {

    private Path workingDirectory = getUserHome();

    private final NativeCommandRunner commandRunner = new NativeCommandRunner();

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
        commandRunner.shutdown();
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
            int exitValue = commandRunner.run( commands, workingDirectory.toFile(), out, err );

            if ( exitValue != 0 ) {
                err.println( "Process exit value: " + exitValue );
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

}
