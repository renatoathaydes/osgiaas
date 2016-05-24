package com.athaydes.osgiaas.api.autoupdate;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Auto-update options for the {@link AutoUpdater} service.
 */
public interface AutoUpdateOptions {

    Duration autoUpdatePeriod();

    List<URI> bundleRepositoryUris();

    default Function<UpdateInformation, Boolean> acceptUpdate() {
        return info -> true;
    }

}
