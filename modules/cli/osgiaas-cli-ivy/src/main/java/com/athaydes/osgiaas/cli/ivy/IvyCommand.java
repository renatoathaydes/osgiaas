package com.athaydes.osgiaas.cli.ivy;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import org.apache.felix.shell.Command;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.report.ResolveReport;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
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

    private IvyFactory ivyFactory = new IvyFactory();

    private ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( INTRANSITIVE_OPTION, "--intransitive" ).end()
            .accepts( DOWNLOAD_ALL_OPTION, "--download-all" ).end()
            .accepts( REPOSITORIES_OPTION, "--repositories" ).withArgCount( 1 ).allowMultiple().end()
            .build();

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
                version = "integration.latest";
            }

            Ivy ivy = getIvy( invocation );

            try {
                ResolveReport resolveReport = new IvyResolver( ivy )
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

    private Ivy getIvy( CommandInvocation invocation ) {
        return ivyFactory.getIvy( invocation.getAllArgumentsFor( REPOSITORIES_OPTION )
                .stream().map( ( it ) -> {
                    try {
                        return new URL( it );
                    } catch ( MalformedURLException e ) {
                        throw new RuntimeException( "Invalid URL: " + it );
                    }
                } ).collect( Collectors.toSet() ) );
    }


}
