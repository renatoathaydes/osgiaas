package com.athaydes.osgiaas.cli.ivy;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import com.athaydes.osgiaas.wrap.JarWrapper;
import org.apache.felix.shell.Command;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;

import javax.annotation.Nullable;
import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command that uses <a href="http://ant.apache.org/ivy/">Apache Ivy</a> to retrieve and resolve artifacts.
 */
public class IvyCommand implements Command {

    static final String NAME = "ivy";
    private static final String INTRANSITIVE_OPTION = "-i";
    private static final String INTRANSITIVE_LONG_OPTION = "--intransitive";
    private static final String REPOSITORIES_OPTION = "-r";
    private static final String REPOSITORIES_LONG_OPTION = "--repository";
    private static final String NO_MAVEN_LOCAL_OPTION = "-n";
    private static final String NO_MAVEN_LOCAL_LONG_OPTION = "--no-maven-local";
    private static final String DOWNLOAD_ALL_OPTION = "-a";
    private static final String DOWNLOAD_ALL_LONG_OPTION = "--download-all";
    private static final String VERBOSE_OPTION = "-v";
    private static final String VERBOSE_LONG_OPTION = "--verbose";

    private final IvyFactory ivyFactory = new IvyFactory();

    static final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( INTRANSITIVE_OPTION, INTRANSITIVE_LONG_OPTION )
            .withDescription( "do not retrieve transitive dependencies" ).end()
            .accepts( VERBOSE_OPTION, VERBOSE_LONG_OPTION )
            .withDescription( "show verbose output" ).end()
            .accepts( DOWNLOAD_ALL_OPTION, DOWNLOAD_ALL_LONG_OPTION )
            .withDescription( "download also javadocs and sources jars if available" ).end()
            .accepts( NO_MAVEN_LOCAL_OPTION, NO_MAVEN_LOCAL_LONG_OPTION )
            .withDescription( "do not use the local Maven repository" ).end()
            .accepts( REPOSITORIES_OPTION, REPOSITORIES_LONG_OPTION ).withArgs( "repo-url" ).allowMultiple()
            .withDescription( "specify repositories to use to search for artifacts (uses JCenter by default)" ).end()
            .build();

    @SuppressWarnings( "unused" ) // called by OSGi
    public void start() {
        ivyFactory.createDefaultConfig();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUsage() {
        return NAME + " " + argsSpec.getUsage() + " group:module[:version]";
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

            boolean verbose = invocation.hasOption( VERBOSE_OPTION );

            if ( verbose ) {
                System.out.printf( "Resolving group=%s, module=%s, version=%s\n",
                        group, module, version );
            }

            @Nullable Ivy ivy = getIvy( invocation, verbose );

            if ( ivy == null ) {
                err.println( "The Ivy command is not fully initialized yet. Try again in a few seconds!" );
                return;
            }

            ivyFactory.getVerbose().set( verbose );

            try {
                ResolveReport resolveReport = new IvyResolver( ivy )
                        .includeTransitiveDependencies( !invocation.hasOption( INTRANSITIVE_OPTION ) )
                        .downloadJarOnly( !invocation.hasOption( DOWNLOAD_ALL_OPTION ) )
                        .verbose( verbose )
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
                            .map( it -> "file://" + wrapIfRequired( it, err ).getAbsolutePath() )
                            .collect( Collectors.joining( " " ) ) );
                }
            } catch ( RuntimeException e ) {
                err.println( e.getCause() );
            }
        }
    }

    private File wrapIfRequired( ArtifactDownloadReport jar, PrintStream err ) {
        String version = jar.getArtifactOrigin().getArtifact().getModuleRevisionId().getRevision();
        JarWrapper wrapper = new JarWrapper( jar.getLocalFile() );

        try {
            return wrapper.wrap( version );
        } catch ( Exception e ) {
            err.println( "Problem trying to wrap jar into OSGi bundle: " + jar );
            e.printStackTrace( err );
            return jar.getLocalFile();
        }
    }

    @Nullable
    private Ivy getIvy( CommandInvocation invocation, boolean verbose ) {
        Set<URL> repositories = invocation.hasOption( REPOSITORIES_OPTION )
                ? invocation.getAllArgumentsFor( REPOSITORIES_OPTION )
                .stream().map( ( it ) -> {
                    try {
                        return new URL( it );
                    } catch ( MalformedURLException e ) {
                        throw new RuntimeException( "Invalid URL: " + it );
                    }
                } ).collect( Collectors.toSet() )
                : null;

        if ( verbose ) {
            if ( repositories == null ) {
                System.out.println( "No remote Ivy repositories are configured, using default JCenter!" );
            } else {
                System.out.println( "Current Ivy repositories: " + String.join( ", ",
                        repositories.stream().map( Object::toString ).toArray( String[]::new ) ) );
            }
        }

        return ivyFactory.getIvy( repositories, !invocation.hasOption( NO_MAVEN_LOCAL_OPTION ) );
    }

}
