package com.athaydes.osgiaas.cli.ivy;

import org.apache.ivy.Ivy;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Creates or return a previously created {@link Ivy} instance depending on which repositories are used.
 */
public class IvyFactory {

    @Nullable
    private Ivy defaultIvy = null;
    @Nullable
    private Ivy configuredIvy = null;

    private Set<URL> currentConfiguredIvyRepos = Collections.emptySet();

    /**
     * Create or re-use an Ivy instance using the provided repositories.
     *
     * @param repositories URL to Maven repositories
     * @return Ivy instance (may be re-used)
     */
    Ivy getIvy( Set<URL> repositories ) {
        if ( repositories.isEmpty() ) {
            return getDefaultIvy();
        } else if ( configuredIvy != null && currentConfiguredIvyRepos.equals( repositories ) ) {
            assert configuredIvy != null;
            return configuredIvy;
        } else {
            return getIvyWith( repositories );
        }
    }

    private Ivy getDefaultIvy() {
        if ( defaultIvy == null ) {
            defaultIvy = Ivy.newInstance();
            assert defaultIvy != null;

            try {
                defaultIvy.configure( IvyFactory.class.getResource( "/ivy-settings.xml" ) );
            } catch ( ParseException | IOException e ) {
                e.printStackTrace();
            }
        }

        return defaultIvy;
    }

    private Ivy getIvyWith( Set<URL> repositories ) {
        configuredIvy = Ivy.newInstance();
        assert configuredIvy != null;

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
                            return xmlForRepositories( repositories );
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

            configuredIvy.configure( tempSettings );
            currentConfiguredIvyRepos = repositories;
        } catch ( ParseException | IOException e ) {
            e.printStackTrace();
        }

        return configuredIvy;
    }

    private String xmlForRepositories( Set<URL> repositories ) {
        return repositories.stream()
                .map( it -> String.format(
                        "<ibiblio name=\"%s-%s\" root=\"%s/\" m2compatible=\"true\"/>",
                        it.getHost(), it.getPath(), it.toString() ) )
                .collect( Collectors.joining() );
    }

}
