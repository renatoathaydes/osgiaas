package com.athaydes.osgiaas.autoupdate;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Auto-update options for the {@link AutoUpdater} service.
 */
public interface AutoUpdateOptions {

    /**
     * @return the update period, ie. the minimum time between each auto-update run.
     * Implementations are free to run an auto-update at any time after the latest auto-update has expired,
     * or at the soonest time if the registered bundle has never been updated.
     */
    Duration autoUpdatePeriod();

    /**
     * @return URIs pointing to repositories supported by Ivy, such as any Maven repository, where the bundle
     * resource can be located.
     */
    List<URI> bundleRepositoryUris();

    /**
     * @return a callback that will be called to determine whether or not an update should occur.
     * The default implementation returns true if the new version is different from the current version of the bundle,
     * false otherwise.
     * <p>
     * Notice that this callback may be used to ask the user whether or not the bundle should be updated.
     */
    default Function<UpdateInformation, Boolean> acceptUpdate() {
        return info -> !info.getNewestVersion().equals( info.getCurrentVersion() );
    }

}
