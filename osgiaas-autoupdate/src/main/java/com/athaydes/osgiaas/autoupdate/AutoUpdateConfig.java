package com.athaydes.osgiaas.autoupdate;

import com.athaydes.osgiaas.api.autoupdate.AutoUpdateOptions;
import com.athaydes.osgiaas.autoupdate.config.AutoUpdateConfigKeys;
import org.osgi.service.cm.ConfigurationException;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link AutoUpdaterService} configuration.
 * The configuration keys used by this class are defined in
 * {@link com.athaydes.osgiaas.autoupdate.config.AutoUpdateConfigKeys}.
 */
public class AutoUpdateConfig implements AutoUpdateOptions, AutoUpdateConfigKeys {

    private static final Duration defaultUpdateFrequency = Duration.ofDays( 7 );
    private static final String[] defaultBundleExcludes = new String[]{ "0" };
    private static final List<URI> defaultRepositories = Collections.singletonList(
            URI.create( "https://jcenter.bintray.com" )
    );

    private final Duration updateFrequency;
    private final List<URI> repositories;
    private final String[] bundleExcludes;

    public AutoUpdateConfig( Duration updateFrequency,
                             List<URI> repositories,
                             String[] bundleExcludes ) {
        this.updateFrequency = updateFrequency;
        this.repositories = repositories;
        this.bundleExcludes = bundleExcludes;
    }

    static AutoUpdateConfig defaultConfig() {
        return new AutoUpdateConfig( defaultUpdateFrequency,
                defaultRepositories,
                defaultBundleExcludes );
    }

    public static AutoUpdateConfig fromDictionary( Dictionary<String, ?> config )
            throws ConfigurationException {
        Duration updateFrequency = valueOrDefault( config.get( UPDATE_FREQUENCY ),
                defaultUpdateFrequency, Duration.class );
        String[] repositories = valueOrDefault( config.get( REPOSITORIES ),
                new String[ 0 ], String[].class );
        // TODO split each String using whitespaces if necessary
        String[] bundleExcludes = valueOrDefault( config.get( BUNDLE_EXCLUDES ),
                defaultBundleExcludes, String[].class );

        List<URI> repositoriesUris;

        if ( repositories.length == 0 ) {
            repositoriesUris = defaultRepositories;
        } else try {
            repositoriesUris = Stream.of( repositories )
                    .map( URI::create )
                    .collect( Collectors.toList() );
        } catch ( IllegalArgumentException e ) {
            throw new ConfigurationException( REPOSITORIES, "Invalid URI", e );
        }

        return new AutoUpdateConfig( updateFrequency, repositoriesUris, bundleExcludes );
    }

    public static AutoUpdateConfig fromSystemProperties()
            throws ConfigurationException {
        @Nullable String updateFrequencyInSeconds = System.getProperty( UPDATE_FREQUENCY );
        @Nullable String repositoriesString = System.getProperty( REPOSITORIES );
        @Nullable String bundlesExcludesString = System.getProperty( BUNDLE_EXCLUDES );

        Duration updateFrequency;
        try {
            updateFrequency = updateFrequencyInSeconds == null ?
                    defaultUpdateFrequency : Duration.ofSeconds( Long.valueOf( updateFrequencyInSeconds ) );
        } catch ( NumberFormatException nfe ) {
            throw new ConfigurationException( UPDATE_FREQUENCY, "Invalid number", nfe );
        }

        List<URI> repositoriesUris;
        try {
            repositoriesUris = repositoriesString == null ?
                    defaultRepositories :
                    Stream.of( repositoriesString.split( "," ) )
                            .map( URI::create )
                            .collect( Collectors.toList() );
        } catch ( IllegalArgumentException e ) {
            throw new ConfigurationException( REPOSITORIES, "Invalid URI", e );
        }

        String[] bundlesExcludes = bundlesExcludesString == null ?
                defaultBundleExcludes :
                bundlesExcludesString.split( " " );

        return new AutoUpdateConfig( updateFrequency, repositoriesUris, bundlesExcludes );
    }

    @Override
    public Duration autoUpdateFrequency() {
        return updateFrequency;
    }

    @Override
    public List<URI> bundleRepositoryUris() {
        return repositories;
    }

    public String[] getBundleExcludes() {
        return bundleExcludes;
    }

    private static <T> T valueOrDefault( @Nullable Object object,
                                         T defaultValue,
                                         Class<T> type ) {
        if ( type.isInstance( object ) ) {
            return type.cast( object );
        } else {
            return defaultValue;
        }
    }

}
