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
    public static final String INTRANSITIVE_LONG_OPTION = "--intransitive";
    public static final String REPOSITORIES_OPTION = "-r";
    public static final String REPOSITORIES_LONG_OPTION = "--repository";
    public static final String NO_MAVEN_LOCAL_OPTION = "-n";
    public static final String NO_MAVEN_LOCAL_LONG_OPTION = "--no-maven-local";
    public static final String DOWNLOAD_ALL_OPTION = "-a";
    public static final String DOWNLOAD_ALL_LONG_OPTION = "--download-all";

    private IvyFactory ivyFactory = new IvyFactory();

    static final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( INTRANSITIVE_OPTION, INTRANSITIVE_LONG_OPTION )
            .withDescription( "do not retrieve transitive dependencies" ).end()
            .accepts( DOWNLOAD_ALL_OPTION, DOWNLOAD_ALL_LONG_OPTION )
            .withDescription( "download also javadocs and sources jars if available" ).end()
            .accepts( NO_MAVEN_LOCAL_OPTION, NO_MAVEN_LOCAL_LONG_OPTION )
            .withDescription( "do not use the local Maven repository" ).end()
            .accepts( REPOSITORIES_OPTION, REPOSITORIES_LONG_OPTION ).withArgs( "repo-url" ).allowMultiple()
            .withDescription( "specify repositories to use to search for artifacts (uses JCenter by default)" ).end()
            .build();

    public void start() {
        ivyFactory.createDefaultConfig();
    }

    @Override
    public String getName() {
        return "ivy";
    }

    @Override
    public String getUsage() {
        return "ivy " + argsSpec.getUsage() + " group:module[:version]";
    }

    @Override
    public String getShortDescription() {
        return "Retrieves Ivy/Maven artifacts using Apache Ivy.\n\n" +
                "The ivy command supports the following options:\n\n" +
                argsSpec.getDocumentation( "  " ) + "\n\n" +
                "Example:\n\n" +
                ">> ivy -i io.javaslang:javaslang:2.1.0-alpha\n" +
                "< file:///home/username/.ivy2/cache/io.javaslang/javaslang/jars/javaslang-2.1.0-alpha.jar" +
                "\n\n" +
                "The artifact's version can be omitted, in which case the latest version is downloaded.\n" +
                "The output of the ivy command is a file URL that can be recognized by the 'install' and 'start'" +
                " commands.\n" +
                "Example to download and immediately start a library:\n\n" +
                ">> ivy io.javaslang:javaslang:2.1.0-alpha | start\n";
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
                version = "latest.integration";
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
        return ivyFactory.getIvy( invocation.hasOption( REPOSITORIES_OPTION )
                ? invocation.getAllArgumentsFor( REPOSITORIES_OPTION )
                .stream().map( ( it ) -> {
                    try {
                        return new URL( it );
                    } catch ( MalformedURLException e ) {
                        throw new RuntimeException( "Invalid URL: " + it );
                    }
                } ).collect( Collectors.toSet() )
                : null, !invocation.hasOption( NO_MAVEN_LOCAL_OPTION ) );
    }

}
