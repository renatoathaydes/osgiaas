package com.athaydes.osgiaas.gradle;

import com.athaydes.osgiaas.api.env.NativeCommandRunner;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OSGiaaS Gradle. Runs Gradle programmatically.
 * <p>
 * By default, the JCenter repository and the local Maven repository are available to resolve dependencies.
 * This can be changed using the {@link #addRepositorySpecification(String)}
 * and {@link #setRepositoriesSpecifications(Set)} methods.
 */
public class OsgiaasGradle {

    private final NativeCommandRunner commandRunner;
    private final String gradleExecutable;
    private final PrintStream out;
    private final PrintStream err;
    private final Set<String> repositories = new HashSet<>( 3 );

    /**
     * Create an instance of this class using System.out and System.err for Gradle output.
     *
     * @param gradleExecutable gradle executable to invoke.
     */
    public OsgiaasGradle( String gradleExecutable, NativeCommandRunner commandRunner ) {
        this( gradleExecutable, commandRunner, System.out, System.err );
    }

    public OsgiaasGradle( String gradleExecutable, NativeCommandRunner commandRunner,
                          PrintStream out, PrintStream err ) {
        this.gradleExecutable = gradleExecutable;
        this.commandRunner = commandRunner;
        this.out = out;
        this.err = err;
        repositories.add( "jcenter()" );
        repositories.add( "mavenLocal()" );
    }

    /**
     * @return the command runner used by this instance.
     */
    public NativeCommandRunner getCommandRunner() {
        return commandRunner;
    }

    /**
     * Set all repositories to use to resolve dependencies.
     * <p>
     * Each item should follow the syntax used in regular Gradle files.
     * For example, to use Maven Central, you can use {@code mavenCentral()}.
     *
     * @param repositoriesSpecifications all repositories to use.
     */
    public void setRepositoriesSpecifications( Set<String> repositoriesSpecifications ) {
        repositories.clear();
        repositories.addAll( repositoriesSpecifications );
    }

    /**
     * Add a repository to use to resolve dependencies.
     * <p>
     * The repository specification should follow the syntax used in regular Gradle files.
     * For example, to add Maven Central, you can use {@code mavenCentral()}.
     *
     * @param repositorySpecification repository to add.
     */
    public void addRepositorySpecification( String repositorySpecification ) {
        repositories.add( repositorySpecification );
    }

    /**
     * Copies the given dependency and, by default, all of its transitive dependencies, to the provided location.
     * <p>
     * If location does not exist, an Exception is thrown.
     * <p>
     * Use options to configure the dependency using the syntax used within configuration Closures
     * in regular Gardle files. For example, to not copy transitive dependencies, add the following option:
     * {@code transitive = false}.
     *
     * @param location   to copy the dependency to.
     * @param dependency root dependency to copy.
     * @param options    dependency configuration options as specified in regular Gradle files.
     * @return status code returned by the Gradle process
     * @throws IllegalArgumentException if location is not a writable directory.
     * @throws RuntimeException         if a problem occurs while attempting to create temporary files to invoke Gradle.
     */
    public int copyDependencyTo( File location, String dependency, String... options ) {
        if ( !location.isDirectory() || !location.canWrite() ) {
            throw new IllegalArgumentException( "Unable to write to the given location: " + location );
        }

        System.out.println( "Copying dependencies to " + location.getAbsolutePath() );

        try {
            Path tempDir = Files.createTempDirectory( "osgiaas-gradle-" );

            writeBuildFileTo( tempDir, dependency, options );

            int status = commandRunner.run( Arrays.asList( gradleExecutable, "createOsgiRuntime" ),
                    tempDir.toFile(), out, err );

            @Nullable File[] bundleFiles = tempDir.resolve( "build/osgi/bundle" ).toFile().listFiles();
            if ( bundleFiles != null ) {
                Stream.of( bundleFiles ).forEach( bundleFile -> {
                    try {
                        Files.copy( bundleFile.toPath(),
                                location.toPath().resolve( bundleFile.getName() ),
                                StandardCopyOption.REPLACE_EXISTING );
                    } catch ( IOException e ) {
                        e.printStackTrace( err );
                    }
                } );
            }

            return status;
        } catch ( IOException | InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Prints the dependency tree of the given dependency.
     * <p>
     * Use options to configure the dependency using the syntax used within configuration Closures
     * in regular Gardle files. For example, to not include dependencies with the 'com.acme', add the following option:
     * {@code exclude group: 'com.acme'}.
     *
     * @param dependency root dependency.
     * @param options    dependency configuration options as specified in regular Gradle files.
     * @return status code returned by the Gradle process
     * @throws RuntimeException if a problem occurs while attempting to create temporary files to invoke Gradle.
     */
    public int printDependencies( String dependency, String... options ) {
        try {
            Path tempDir = Files.createTempDirectory( "osgiaas-gradle-" );

            writeBuildFileTo( tempDir, dependency, options );

            return commandRunner.run( Arrays.asList( gradleExecutable,
                    "dependencies", "--configuration", "osgiRuntime" ),
                    tempDir.toFile(), out, err );
        } catch ( IOException | InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }

    private void writeBuildFileTo( Path tempDir, String dependency, String[] options )
            throws IOException {
        String indentedRepositories = String.join( "\n", indent( repositories, 1 ) );

        String optionsText = options.length == 0 ?
                "" :
                ", {\n" + String.join( "\n", indent( Arrays.asList( options ), 2 ) ) + "\n  }";

        String buildScript = "plugins {\n" +
                "    id \"com.athaydes.osgi-run\" version \"1.5.2\"\n" +
                "}\n" +
                "repositories {\n" + indentedRepositories + "\n}\n" +
                "dependencies {\n" +
                "  osgiRuntime \"" + dependency + "\"" + optionsText + "\n" +
                "}\n" +
                "runOsgi {\n" +
                "  bundles = []\n" +
                "}\n";

        Path buildFile = tempDir.resolve( "build.gradle" );
        System.out.println( "Writing build file to " + buildFile.toAbsolutePath() );

        Files.write( buildFile, buildScript.getBytes( StandardCharsets.UTF_8 ) );
    }

    private static List<String> indent( Collection<String> entries, int count ) {
        Function<String, String> indentFunction = ( text ) -> {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < count; i++) {
                builder.append( "  " );
            }
            builder.append( text );
            return builder.toString();
        };

        return entries.stream().map( indentFunction ).collect( Collectors.toList() );
    }

}
