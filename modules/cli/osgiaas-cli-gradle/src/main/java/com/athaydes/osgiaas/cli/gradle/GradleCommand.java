package com.athaydes.osgiaas.cli.gradle;

import com.athaydes.osgiaas.api.env.NativeCommandRunner;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.gradle.OsgiaasGradle;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command that invokes the Gradle module to use Gradle from the CLI.
 */
public class GradleCommand implements Command {

    private final NativeCommandRunner commandRunner = new NativeCommandRunner();

    @Nullable
    private Path bundleLocation;

    public void stop() {
        commandRunner.shutdown();
    }

    @Nullable
    private Path getBundleLocation( PrintStream err ) {
        if ( bundleLocation != null ) {
            return bundleLocation;
        }
        try {
            bundleLocation = Files.createTempDirectory( "osgiaas-cli-gradle-" );
        } catch ( IOException e ) {
            e.printStackTrace( err );
        }

        return bundleLocation;
    }

    @Override
    public String getName() {
        return "gradle";
    }

    @Override
    public String getUsage() {
        return "gradle task";
    }

    @Override
    public String getShortDescription() {
        return "Runs Gradle tasks";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        @Nullable Path location = getBundleLocation( err );

        if ( location == null ) {
            return;
        }

        List<String> arguments = CommandHelper.breakupArguments( line, 4 );

        if ( arguments.size() != 3 ) {
            CommandHelper.printError( err, getUsage(), "Wrong number of arguments" );
        } else {
            String subCommand = arguments.get( 1 );
            String dependency = arguments.get( 2 );

            OsgiaasGradle gradle = new OsgiaasGradle( "gradle", commandRunner, out, err );

            switch ( subCommand ) {
                case "get":
                    gradle.copyDependencyTo( location.toFile(), dependency );
                    break;
                case "show":
                    gradle.printDependencies( dependency );
                    break;
                default:
                    CommandHelper.printError( err, getUsage(), "Unknown sub-command: " + subCommand );
            }
        }
    }
}
