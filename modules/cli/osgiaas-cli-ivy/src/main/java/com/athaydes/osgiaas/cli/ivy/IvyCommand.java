package com.athaydes.osgiaas.cli.ivy;

import com.athaydes.osgiaas.cli.CommandHelper;
import org.apache.felix.shell.Command;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.util.filter.Filter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command that uses <a href="http://ant.apache.org/ivy/">Apache Ivy</a> to retrieve and resolve artifacts.
 */
public class IvyCommand implements Command {

    @Nullable
    private Ivy defaultIvy = null;

    private Ivy getDefaultIvy() {
        if ( defaultIvy == null ) {
            defaultIvy = Ivy.newInstance();
            assert defaultIvy != null;

            try {
                defaultIvy.configure( getClass().getResource( "/ivy-settings.xml" ) );
            } catch ( ParseException | IOException e ) {
                e.printStackTrace();
            }
        }

        return defaultIvy;
    }

    public ResolveReport resolve( String group, String module, String version ) {
        Ivy ivy = getDefaultIvy();

        DefaultModuleDescriptor moduleDescriptor =
                DefaultModuleDescriptor.newDefaultInstance( ModuleRevisionId.newInstance( group,
                        module + "-caller", "working" ) );

        moduleDescriptor.addConfiguration( new Configuration( "default" ) );

        DefaultDependencyDescriptor dependencyDescriptor = new DefaultDependencyDescriptor( moduleDescriptor,
                ModuleRevisionId.newInstance( group, module, version ), false, false, true );

        moduleDescriptor.addDependency( dependencyDescriptor );

        try {
            File ivyfile = File.createTempFile( "ivy", ".xml" );
            ivyfile.deleteOnExit();

            XmlModuleDescriptorWriter.write( moduleDescriptor, ivyfile );

            Filter jarsOnly = ( obj ) -> ( ( obj instanceof Artifact ) &&
                    "jar".equals( ( ( Artifact ) obj ).getType() ) );

            return ivy.resolve( ivyfile.toURI().toURL(), new ResolveOptions()
                    .setConfs( new String[]{ "default" } )
                    .setArtifactFilter( jarsOnly )
                    .setTransitive( true ) );
        } catch ( ParseException | IOException e ) {
            throw new RuntimeException( e );
        }
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
                    try {
                        ResolveReport resolveReport = resolve(
                                dependencyParts[ 0 ], dependencyParts[ 1 ], dependencyParts[ 2 ] );

                        if ( resolveReport.hasError() ) {
                            if ( resolveReport.getProblemMessages().isEmpty() ) {
                                err.println( "Unable to resolve dependency. Are you sure it exists?" );
                            } else {
                                err.println( "Errors resolving dependency:" );
                                for (Object problem : resolveReport.getProblemMessages()) {
                                    out.println( "- " + problem );
                                }
                            }
                        } else {
                            out.println( Stream.of( resolveReport.getAllArtifactsReports() )
                                    .map( it -> "file://" + it.getLocalFile().getAbsolutePath() )
                                    .collect( Collectors.joining( " " ) ) );
                        }
                    } catch ( RuntimeException e ) {
                        err.println( e.getCause() );
                    }
                    break;
                default:
                    CommandHelper.printError( err, getUsage(), "Unknown sub-command: " + subCommand );
            }
        }
    }

}
