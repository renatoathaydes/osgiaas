package com.athaydes.osgiaas.api.autoupdate;

/**
 * Update information provided to client code before an update is applied.
 */
public interface UpdateInformation {

    long getBundleId();

    String getCurrentVersion();

    String getNewestVersion();

}
