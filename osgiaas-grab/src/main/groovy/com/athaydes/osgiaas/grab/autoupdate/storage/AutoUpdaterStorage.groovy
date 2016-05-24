package com.athaydes.osgiaas.grab.autoupdate.storage

import java.time.Duration

/**
 * A local service for bundle update information storage.
 */
interface AutoUpdaterStorage {

    boolean requiresUpdate( String bundleSymbolicName, Duration updatePeriod )

    void markAsUpdated( String... bundleSymbolicNames )

}
