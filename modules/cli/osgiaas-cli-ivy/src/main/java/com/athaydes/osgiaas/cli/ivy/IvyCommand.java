package com.athaydes.osgiaas.cli.ivy;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import org.apache.felix.shell.Command;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.report.ResolveReport;

import javax.annotation.Nullable;
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

    public static final String INTRANSITIVE_OPTION = "-i";
    public static final String REPOSITORIES_OPTION = "-r";
    public static final String DOWNLOAD_ALL_OPTION = "-a";

    @Nullable
    private Ivy defaultIvy = null;

    private ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( INTRANSITIVE_OPTION, "--intransitive" ).end()
            .accepts( DOWNLOAD_ALL_OPTION, "--download-all" ).end()
            .accepts( REPOSITORIES_OPTION, "--repositories" ).withArgCount( 1 ).end()
            .build();

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
        CommandInvocation invocation = argsSpec.parse( line );

        List<String> arguments = CommandHelper.breakupArguments( invocation.getUnprocessedInput(), 2 );

        if ( arguments.size() != 1 ) {
            CommandHelper.printError( err, getUsage(), "Wrong number of arguments" );
        } else {
            String dependency = arguments.get( 0 );

            String[] dependencyParts = dependency.split( ":" );

            String group, module, version;

            if ( dependencyParts.length != 2 && dependencyParts.length != 3 ) {
                CommandHelper.printError( err, getUsage(), "Invalid artifact description. " +
                        "Must follow pattern group:module[:version]" );
                return; // done
            }

            group = dependencyParts[ 0 ];
            module = dependencyParts[ 1 ];

            if ( dependencyParts.length == 3 ) {
                version = dependencyParts[ 2 ];
            } else {
                version = "latest";
            }

            try {
                // TODO get another Ivy if repositories are specified
                ResolveReport resolveReport = new IvyResolver( getDefaultIvy() )
                        .includeTransitiveDependencies( !invocation.hasOption( INTRANSITIVE_OPTION ) )
                        .downloadJarOnly( !invocation.hasOption( DOWNLOAD_ALL_OPTION ) )
                        .resolve( group, module, version );

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
        }
    }

}
