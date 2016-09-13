package com.athaydes.osgiaas.grab.autoupdate

import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.autoupdate.AutoUpdateOptions
import com.athaydes.osgiaas.autoupdate.AutoUpdater
import com.athaydes.osgiaas.autoupdate.UpdateInformation
import com.athaydes.osgiaas.grab.GrabException
import com.athaydes.osgiaas.grab.Grabber
import com.athaydes.osgiaas.grab.MavenVersion
import com.athaydes.osgiaas.grab.autoupdate.storage.AutoUpdaterStorage
import com.athaydes.osgiaas.grab.autoupdate.storage.UserHomeFileAutoUpdaterStorage
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.osgi.framework.Bundle
import org.osgi.framework.wiring.FrameworkWiring
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Nullable
import java.util.concurrent.atomic.AtomicReference

@Immutable
class GrabUpdateInformation implements UpdateInformation {
    long bundleId
    String currentVersion
    String newestVersion
}

/**
 * AutoUpdater based on {@link com.athaydes.osgiaas.grab.Grabber}.
 */
@CompileStatic
class GrabAutoUpdater implements AutoUpdater {

    static final Logger log = LoggerFactory.getLogger( GrabAutoUpdater )

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>()
    private final AutoUpdaterStorage storage = new UserHomeFileAutoUpdaterStorage()

    private final Set<Long> subscribedBundles = [ ].asSynchronized() as Set<Long>

    void activate( ComponentContext context ) {
        contextRef.set( context )
    }

    void deactivate( ComponentContext context ) {
        contextRef.set( null )
    }

    @Override
    void subscribeBundle( String bundleSymbolicName, AutoUpdateOptions options ) {
        DynamicServiceHelper.with( contextRef, { ComponentContext context ->
            @Nullable Bundle bundle = context.bundleContext.bundles.find {
                it.headers[ 'Bundle-SymbolicName' ] == bundleSymbolicName
            }
            if ( bundle ) {
                log.info( "Subscribing bundle {} for auto-update", bundleSymbolicName )
                def success = update bundle, options
                if ( success ) {
                    refreshPendingBundles context
                }
            } else {
                log.info "Unable to subscribe bundle {} as it is not" +
                        " currently installed.", bundleSymbolicName
            }
        }, {
            log.warn "Unable to subscribe bundle {} for autoupdate" +
                    " as the ComponentContext is not currently available", bundleSymbolicName
        } )
    }

    @Override
    void subscribeBundle( long bundleId, AutoUpdateOptions options ) {
        DynamicServiceHelper.with( contextRef ) { ComponentContext context ->
            def bundle = context.bundleContext.getBundle( bundleId )
            if ( bundle ) {
                log.info( "Checking if bundle {} can be auto-updated", bundle.bundleId )
                def success = update bundle, options
                if ( success ) {
                    refreshPendingBundles context
                }
            } else {
                log.warn "Bundle with ID {} does not exist", bundleId
            }
        }
    }

    @Override
    void subscribeAllBundles( AutoUpdateOptions options, String... exclusions ) {
        // TODO use exclusions!
        DynamicServiceHelper.with( contextRef ) { ComponentContext context ->
            log.info( "Subscribing all bundles for auto-update" )
            def success = false
            for ( bundle in context.bundleContext.bundles ) {
                success |= update bundle, options
            }
            if ( success ) {
                refreshPendingBundles context
            }
        }
    }

    private boolean update( Bundle bundle,
                            AutoUpdateOptions options ) {
        if ( bundle.bundleId in subscribedBundles ) {
            return false
        }

        @Nullable
        String bundleName = bundle.headers.get( 'Bundle-SymbolicName' )
        @Nullable
        String bundleVersion = bundle.headers.get( 'Bundle-Version' )
        @Nullable
        String bundleCoordinates = bundle.headers.get( 'Osgiaas-Bundle-Coordinates' )

        @Nullable String failureReason = null

        if ( !bundleName ) {
            failureReason = "Not a valid bundle! Manifest missing the 'Bundle-SymbolicName' entry. ID=${bundle.bundleId}"
        } else if ( !bundleVersion ) {
            failureReason = "Manifest missing the 'Bundle-Version' entry. ID=${bundle.bundleId}"
        } else if ( bundleCoordinates ) {
            def requiresUpdate = storage.requiresUpdate( bundleName, options.autoUpdatePeriod() )

            if ( !requiresUpdate ) {
                log.info( "Bundle [{}] already up-to-date", bundleCoordinates )
                return false
            }

            log.info( "Checking updates for bundle at coordinates: {}", bundleCoordinates )

            try {
                return grabAndUpdate( bundleCoordinates, bundleVersion, bundleName, bundle, options )
            } catch ( e ) {
                log.warn( "Unable to auto-update bundle {} due to {}", bundleCoordinates, e )
            }
        } else {
            log.debug "Bundle manifest does not have a 'Osgiaas-Bundle-Coordinates' entry." +
                    " Add the entry in the manifest so that the coordinates of the bundle in the Maven" +
                    " repository can be determined."
        }

        if ( failureReason ) {
            log.info( "Cannot auto-update bundle [{}]: {}", bundle.bundleId, failureReason )
        }

        return false
    }

    boolean grabAndUpdate( String bundleCoordinates, bundleVersion, String bundleName,
                           Bundle bundle, AutoUpdateOptions options ) {
        def grabResult = new Grabber( repositories( options.bundleRepositoryUris() ) )
                .grab( newestCoordinatesFor( bundleCoordinates ) )

        def bundleGrape = grabResult.grapeFile

        // canonicalize the versions
        def currentVersion = MavenVersion.parseVersionString( bundleVersion.toString() )
        def newVersion = MavenVersion.parseVersionString( grabResult.grapeVersion )

        log.debug( "Bundle current version: {}. New version: {}", currentVersion, newVersion )

        if ( newVersion > currentVersion ) {
            def doUpdate = options.acceptUpdate().apply( new GrabUpdateInformation(
                    bundle.bundleId, currentVersion.toString(), newVersion.toString()
            ) )
            if ( doUpdate ) {
                log.info( "Auto-update approved for bundle {}. " +
                        "New version: {}", bundleCoordinates, grabResult.grapeVersion )
                try {
                    bundleGrape.withInputStream { bundle.update( it ) }
                    subscribedBundles << bundle.bundleId
                    storage.markAsUpdated( bundleName )
                    log.info( "Bundle has been updated to version {}: {}",
                            grabResult.grapeVersion, bundleCoordinates )
                    return true
                } catch ( e ) {
                    throw new GrabException( e.toString() )
                }
            } else {
                log.info( "Auto-update not accepted for bundle {}", bundleCoordinates )
            }
        } else {
            log.debug( "Bundle is already up-to-date: {}", bundleCoordinates )
        }

        return false
    }

    String newestCoordinatesFor( String coordinates ) {
        String[] parts = coordinates.split( ':' )
        if ( parts.size() > 2 ) {
            return "${parts[ 0 ]}:${parts[ 1 ]}:[${parts[ 2 ]},)"
        } else {
            log.warn( "Cannot resolve newer coordinates. Invalid coordinates: {}", coordinates )
            return coordinates
        }
    }

    private static Map<String, String> repositories( List<URI> uris ) {
        uris.collectEntries { [ ( it.toASCIIString() ): it.toString() ] }
    }

    private static void refreshPendingBundles( ComponentContext context ) {
        def systemBundle = context.bundleContext.getBundle( 0L )
        systemBundle.adapt( FrameworkWiring ).refreshBundles( null )
    }
}
