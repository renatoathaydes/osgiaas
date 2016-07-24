package com.athaydes.osgiaas.api.autoupdate;

/**
 * Update information provided to client code before an update is applied.
 */
public interface UpdateInformation {

    /**
     * @return the ID of the bundle to be updated.
     */
    long getBundleId();

    /**
     * @return the current version of the installed bundle.
     */
    String getCurrentVersion();

    /**
     * @return the newer version of the bundle.
     */
    String getNewestVersion();

}
