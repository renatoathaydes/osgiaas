package com.athaydes.osgiaas.grab.autoupdate

import aQute.bnd.version.MavenVersion
import com.athaydes.osgiaas.api.autoupdate.AutoUpdateOptions
import com.athaydes.osgiaas.api.autoupdate.AutoUpdater
import com.athaydes.osgiaas.api.autoupdate.UpdateInformation
import com.athaydes.osgiaas.api.service.DynamicServiceHelper
import com.athaydes.osgiaas.grab.GrabException
import com.athaydes.osgiaas.grab.Grabber
import com.athaydes.osgiaas.grab.autoupdate.storage.AutoUpdaterStorage
import com.athaydes.osgiaas.grab.autoupdate.storage.UserHomeFileAutoUpdaterStorage
import groovy.transform.Immutable
import org.osgi.framework.Bundle
import org.osgi.framework.wiring.FrameworkWiring
import org.osgi.service.component.ComponentContext
import org.osgi.service.log.LogService

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
class GrabAutoUpdater implements AutoUpdater {

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>()
    private final AtomicReference<LogService> logServiceRef = new AtomicReference<>()
    private final AutoUpdaterStorage storage = new UserHomeFileAutoUpdaterStorage()

    private final Set<Long> subscribedBundles = [ ].asSynchronized()

    void setLogService( LogService logService ) {
        logServiceRef.set( logService )
    }

    void unsetLogService( LogService logService ) {
        logServiceRef.set( null )
    }

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
                log( LogService.LOG_INFO, "Subscribing bundle ${bundleSymbolicName} for auto-update" )
                def success = update bundle, options
                if ( success ) {
                    refreshPendingBundles context
                }
            } else {
                log LogService.LOG_INFO, "Unable to subscribe bundle $bundleSymbolicName as it is not" +
                        " currently installed."
            }
        }, {
            log LogService.LOG_WARNING, "Unable to subscribe bundle $bundleSymbolicName for autoupdate" +
                    " as the ComponentContext is not currently available"
        } )
    }

    @Override
    void subscribeBundle( long bundleId, AutoUpdateOptions options ) {
        DynamicServiceHelper.with( contextRef ) { ComponentContext context ->
            def bundle = context.bundleContext.getBundle( bundleId )
            if ( bundle ) {
                log( LogService.LOG_INFO, "Checking if bundle ${bundle.bundleId} can be auto-updated" )
                def success = update bundle, options
                if ( success ) {
                    refreshPendingBundles context
                }
            } else {
                log LogService.LOG_WARNING, "Bundle with ID $bundleId does not exist"
            }
        }
    }

    @Override
    void subscribeAllBundles( AutoUpdateOptions options, String... exclusions ) {
        DynamicServiceHelper.with( contextRef ) { ComponentContext context ->
            log( LogService.LOG_INFO, "Subscribing all bundles for auto-update" )
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
                log( LogService.LOG_INFO, "Bundle [$bundleCoordinates] already up-to-date" )
                return false
            }

            log( LogService.LOG_INFO, "Checking updates for bundle at coordinates: $bundleCoordinates" )

            try {
                return grabAndUpdate( bundleCoordinates, bundleVersion, bundleName, bundle, options )
            } catch ( e ) {
                log( LogService.LOG_WARNING, "Unable to auto-update bundle $bundleCoordinates due to $e" )
            }
        } else {
            log LogService.LOG_DEBUG, "Bundle manifest does not have a 'Osgiaas-Bundle-Coordinates' entry." +
                    " Add the entry in the manifest so that the coordinates of the bundle in the Maven" +
                    " repository can be determined."
        }

        if ( failureReason ) {
            log( LogService.LOG_INFO, "Cannot auto-update bundle [${bundle.bundleId}]: $failureReason" )
        }

        return false
    }

    boolean grabAndUpdate( String bundleCoordinates, bundleVersion, String bundleName,
                           Bundle bundle, AutoUpdateOptions options ) {
        def grabResult = new Grabber( repositories( options.bundleRepositoryUris() ) )
                .grab( newestCoordinatesFor( bundleCoordinates ) )

        def bundleGrape = grabResult.grapeFile

        // canonicalize the versions
        def currentVersion = MavenVersion.parseString( bundleVersion.toString() )
        def newVersion = MavenVersion.parseString( grabResult.grapeVersion )

        log( LogService.LOG_DEBUG, "Bundle current version: $currentVersion. New version: $newVersion" )

        if ( newVersion > currentVersion ) {
            def doUpdate = options.acceptUpdate().apply( new GrabUpdateInformation(
                    bundle.bundleId, currentVersion.toString(), newVersion.toString()
            ) )
            if ( doUpdate ) {
                log( LogService.LOG_INFO, "Auto-update approved for bundle $bundleCoordinates. " +
                        "New version: ${grabResult.grapeVersion}" )
                try {
                    bundleGrape.withInputStream { bundle.update( it ) }
                    subscribedBundles << bundle.bundleId
                    storage.markAsUpdated( bundleName )
                    log( LogService.LOG_INFO, "Bundle has been updated to version " +
                            "${grabResult.grapeVersion}: $bundleCoordinates" )
                    return true
                } catch ( e ) {
                    throw new GrabException( e.toString() )
                }
            } else {
                log( LogService.LOG_INFO, "Auto-update not accepted for bundle $bundleCoordinates" )
            }
        } else {
            log( LogService.LOG_DEBUG, "Bundle is already up-to-date: $bundleCoordinates" )
        }

        return false
    }

    String newestCoordinatesFor( String coordinates ) {
        String[] parts = coordinates.split( ':' )
        if ( parts.size() > 2 ) {
            return "${parts[ 0 ]}:${parts[ 1 ]}:[${parts[ 2 ]},)"
        } else {
            log( LogService.LOG_WARNING, "Cannot resolve newer coordinates. Invalid coordinates: $coordinates" )
            return coordinates
        }
    }

    private void log( int logLevel, String message ) {
        DynamicServiceHelper.with( logServiceRef ) { LogService logger ->
            logger.log( logLevel, message )
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
