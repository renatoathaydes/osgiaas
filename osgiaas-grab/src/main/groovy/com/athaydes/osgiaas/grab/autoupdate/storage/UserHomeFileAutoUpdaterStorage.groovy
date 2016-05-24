package com.athaydes.osgiaas.grab.autoupdate.storage

import groovy.json.JsonSlurper

import java.time.Duration
import java.time.Instant

import static groovy.json.JsonOutput.toJson

/**
 * Implementation of {@link AutoUpdaterStorage} that uses the user home directory
 * to save information.
 */
class UserHomeFileAutoUpdaterStorage implements AutoUpdaterStorage {

    private final File storageFile
    private final JsonSlurper jsonParser = new JsonSlurper()

    UserHomeFileAutoUpdaterStorage() {
        def userHome = System.getProperty( 'user.home', '.' )
        this.storageFile = new File( userHome, '.osgiaas_grab_auto_updater' )
    }

    @Override
    boolean requiresUpdate( String bundleSymbolicName, Duration updatePeriod ) {
        if ( storageFile.file ) {
            try {
                def json = parseStorageFile()
                def latestUpdate = toInstant( json[ bundleSymbolicName ] ?: '0' )
                def nextUpdate = latestUpdate + updatePeriod
                return Instant.now().isAfter( nextUpdate )
            } catch ( ignore ) {
                // if the file is invalid, wipe it out
                storageFile.delete()
            }
        }

        return true
    }

    private Map parseStorageFile() {
        jsonParser.parse( storageFile, 'UTF-8' ) as Map
    }

    private static Instant toInstant( value ) {
        Instant.ofEpochSecond( value.toString().toLong() )
    }

    @Override
    void markAsUpdated( String... bundleSymbolicNames ) {
        def json = parseStorageFile()
        def now = Instant.now().epochSecond
        for ( name in bundleSymbolicNames ) {
            try {
                json[ name ] = now
            } catch ( e ) {
                e.printStackTrace()
            }
        }

        try {
            storageFile.write toJson( json )
        } catch ( e ) {
            e.printStackTrace()
        }
    }


}
