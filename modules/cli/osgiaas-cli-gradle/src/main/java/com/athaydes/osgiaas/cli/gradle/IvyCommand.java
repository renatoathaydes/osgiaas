package com.athaydes.osgiaas.cli.gradle;

import com.athaydes.osgiaas.api.env.NativeCommandRunner;
import com.athaydes.osgiaas.cli.CommandHelper;
import org.apache.felix.shell.Command;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command that uses Apache Ivy to retrieve and resolve artifacts.
 */
public class IvyCommand implements Command {

    private final NativeCommandRunner commandRunner = new NativeCommandRunner();

    public void resolve( String group, String dep1, String version, PrintStream out, PrintStream err ) {
        IvySettings ivySettings = new IvySettings();
        String homeDir = System.getProperty( "user.home" );
        if ( homeDir == null ) {
            homeDir = "/temp";
        }
        File baseDir = new File( "/.ivy" );
        ivySettings.setBaseDir( baseDir );

        Ivy ivy = Ivy.newInstance( ivySettings );
        try {
            ivy.configure( new File( "/Users/renato/.groovy/grapeConfig.xml" ) );
        } catch ( ParseException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        String[] dep = null;
        dep = new String[]{ group, dep1, version };


        DefaultModuleDescriptor module =
                DefaultModuleDescriptor.newDefaultInstance( ModuleRevisionId.newInstance( dep[ 0 ],
                        dep[ 1 ] + "-caller", "working" ) );

        module.addConfiguration( new Configuration( "default" ) );

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor( module,
                ModuleRevisionId.newInstance( dep[ 0 ], dep[ 1 ], dep[ 2 ] ), false, false, true );

        module.addDependency( dd );


        try {
            File ivyfile = File.createTempFile( "ivy", ".xml" );
            ivyfile.deleteOnExit();
            XmlModuleDescriptorWriter.write( module, ivyfile );

            // stop downloading sources and javadocs
            ResolveReport resolveReport = ivy.resolve( ivyfile.toURL(), new ResolveOptions()
                    .setConfs( new String[]{ "default" } )
                    .setTransitive( true ) );

            if ( resolveReport.hasError() ) {
                err.println( "Errors: " + resolveReport.getProblemMessages() );
            } else {
                out.println( "Artifacts: " + Stream.of( resolveReport.getAllArtifactsReports() )
                        .map( it -> it.getLocalFile().getAbsolutePath() ).collect( Collectors.toList() ) );
            }
        } catch ( ParseException | IOException e ) {
            e.printStackTrace( err );
        }
    }

    public void stop() {
        commandRunner.shutdown();
    }

    @Override
    public String getName() {
        return "ivy";
    }

    @Override
    public String getUsage() {
        return "ivy <sub-command>";
    }

    @Override
    public String getShortDescription() {
        return "Resolve or retrieve artifacts using Apache Ivy";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        List<String> arguments = CommandHelper.breakupArguments( line, 4 );

        if ( arguments.size() != 3 ) {
            CommandHelper.printError( err, getUsage(), "Wrong number of arguments" );
        } else {
            String subCommand = arguments.get( 1 );
            String dependency = arguments.get( 2 );

            String[] dependencyParts = dependency.split( ":" );

            switch ( subCommand ) {
                case "get":
                    resolve( dependencyParts[ 0 ], dependencyParts[ 1 ], dependencyParts[ 2 ], out, err );
                    break;
                case "show":

                    break;
                default:
                    CommandHelper.printError( err, getUsage(), "Unknown sub-command: " + subCommand );
            }
        }
    }

    public static void main( String[] args ) {
        new IvyCommand().execute(
                "ivy get com.athaydes.osgiaas:osgiaas-cli-gradle:1.0-SNAPSHOT",
                System.out, System.err );
    }
}
