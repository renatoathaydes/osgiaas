package com.athaydes.osgiaas.cli.ivy;

import org.apache.ivy.Ivy;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Creates or return a previously created {@link Ivy} instance depending on which repositories are used.
 */
class IvyFactory {

    private static final String JCENTER = "https://jcenter.bintray.com";

    private static final String LOCAL_M2_REPOSITORY = "<ibiblio name=\"localm2\" " +
            "root=\"file:${user.home}/.m2/repository/\" checkmodified=\"true\" " +
            "changingPattern=\".*\" changingMatcher=\"regexp\" m2compatible=\"true\"/>";

    private final Set<URL> defaultRepositories;
    private final Map<RepositoryConfig, Ivy> ivyByConfig = new ConcurrentHashMap<>( 4 );
    private final AtomicBoolean ready = new AtomicBoolean( false );

    IvyFactory() {
        URL jcenterURL;
        try {
            jcenterURL = new URL( JCENTER );
        } catch ( MalformedURLException e ) {
            throw new IllegalStateException( "JCenter URL constant has an invalid value", e );
        }

        defaultRepositories = Collections.singleton( jcenterURL );
    }

    /**
     * Create the default {@link Ivy} instance with the default repositories and using Maven local.
     */
    void createDefaultConfig() {
        RepositoryConfig defaultConfig = new RepositoryConfig( defaultRepositories, true );
        ivyByConfig.put( defaultConfig, createIvyWith( defaultConfig ) );
        ready.set( true );
    }

    /**
     * Create or re-use an Ivy instance using the provided repositories.
     *
     * @param repositories      URL to Maven repositories or null to use the default repositories (JCenter).
     * @param includeMavenLocal include the Maven local repository
     * @return Ivy instance (may be re-used) or null if this factory is not ready yet.
     */
    @Nullable
    Ivy getIvy( @Nullable Set<URL> repositories, boolean includeMavenLocal ) {
        if ( !ready.get() ) {
            return null;
        }

        if ( repositories == null ) {
            repositories = defaultRepositories;
        }

        RepositoryConfig config = new RepositoryConfig( repositories, includeMavenLocal );

        if ( !ivyByConfig.containsKey( config ) ) {
            ivyByConfig.put( config, createIvyWith( config ) );
        }

        return ivyByConfig.get( config );
    }

    private Ivy createIvyWith( RepositoryConfig config ) {
        Ivy ivy = Ivy.newInstance();

        AtomicBoolean repositoriesFound = new AtomicBoolean( false );

        try {
            File tempSettings = File.createTempFile( "ivy-settings-", ".xml" );

            try ( FileWriter writer = new FileWriter( tempSettings );
                  BufferedReader buffer = new BufferedReader(
                          new InputStreamReader( getClass().getResourceAsStream( "/ivy-template.xml" ) ) ) ) {

                buffer.lines().map( it -> {
                    if ( !repositoriesFound.get() ) {
                        if ( it.matches( "\\s*\\$\\{\\s*REPOSITORIES\\s*}\\s*" ) ) {
                            repositoriesFound.set( true );
                            return xmlForRepositories( config );
                        }
                    }

                    return it;
                } ).forEach( line -> {
                    try {
                        writer.append( line ).append( "\n" );
                    } catch ( IOException e ) {
                        e.printStackTrace();
                    }
                } );
            }

            ivy.configure( tempSettings );
        } catch ( ParseException | IOException e ) {
            e.printStackTrace();
        }

        return ivy;
    }

    private String xmlForRepositories( RepositoryConfig config ) {
        String localRepo = config.useMavenLocal ? LOCAL_M2_REPOSITORY : "";
        return localRepo + config.configuredIvyRepos.stream()
                .map( it -> String.format(
                        "<ibiblio name=\"%s\" root=\"%s\" m2compatible=\"true\"/>",
                        it.getHost(), it.toString() ) )
                .collect( Collectors.joining() );
    }

    private static class RepositoryConfig {
        Set<URL> configuredIvyRepos;
        boolean useMavenLocal;

        RepositoryConfig( Set<URL> configuredIvyRepos, boolean useMavenLocal ) {
            this.configuredIvyRepos = configuredIvyRepos;
            this.useMavenLocal = useMavenLocal;
        }

        @Override
        public boolean equals( Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            RepositoryConfig that = ( RepositoryConfig ) o;

            return useMavenLocal == that.useMavenLocal &&
                    configuredIvyRepos.equals( that.configuredIvyRepos );
        }

        @Override
        public int hashCode() {
            int result = configuredIvyRepos.hashCode();
            result = 31 * result + ( useMavenLocal ? 1 : 0 );
            return result;
        }

        @Override
        public String toString() {
            return "RepositoryConfig{" +
                    "configuredIvyRepos=" + configuredIvyRepos +
                    ", useMavenLocal=" + useMavenLocal +
                    '}';
        }
    }

}
