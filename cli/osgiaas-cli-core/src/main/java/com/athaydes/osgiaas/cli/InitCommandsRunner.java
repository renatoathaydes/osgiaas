package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.api.stream.NoOpPrintStream;
import com.athaydes.osgiaas.cli.util.OsgiaasPrintStream;

import javax.annotation.Nullable;
import java.io.File;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Scanner;
import java.util.function.Consumer;

class InitCommandsRunner {

    static void scheduleInitCommands( CliProperties cliProperties,
                                      CommandRunner commandRunner,
                                      Consumer<String> showStatus ) {
        final Duration initCommandTimeout = Duration.ofSeconds( 5 );
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
                showStatus.accept( "Running init commands" );
                Thread.sleep( 250L ); // allows commands to be loaded

                Scanner fileScanner = new Scanner( initFile );

                PrintStream out = new NoOpPrintStream();
                OsgiaasPrintStream err = new OsgiaasPrintStream(
                        System.err, cliProperties.getErrorColor() );

                int index = 1;

                while ( fileScanner.hasNextLine() ) {
                    showStatus.accept( "Running init commands [" + index + "]" );
                    commandRunner.runWhenAvailable( fileScanner.nextLine(), out, err, initCommandTimeout );
                    index++;
                }
            }
        } catch ( Exception e ) {
            System.err.println( "Unable to load osgiaas-cli history: " + e );
        } finally {
            showStatus.accept( "" );
        }

    }

}
