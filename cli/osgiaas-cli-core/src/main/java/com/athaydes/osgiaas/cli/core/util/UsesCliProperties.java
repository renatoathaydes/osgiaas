package com.athaydes.osgiaas.cli.core.util;

import com.athaydes.osgiaas.api.service.DynamicServiceHelper;
import com.athaydes.osgiaas.cli.CliProperties;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A little base class for classes that use the CliProperties service.
 */
public class UsesCliProperties {

    private final AtomicReference<CliProperties> cliProperties = new AtomicReference<>();

    public void setCliProperties( CliProperties cli ) {
        this.cliProperties.set( cli );
    }

    public void removeCliProperties( CliProperties cli ) {
        this.cliProperties.set( null );
    }

    protected void withCliProperties( Consumer<CliProperties> consumer ) {
        DynamicServiceHelper.with( cliProperties, consumer );
    }

    protected void withCliProperties( Consumer<CliProperties> consumer,
                                      Runnable onUnavailable ) {
        DynamicServiceHelper.with( cliProperties, consumer, onUnavailable );
    }

}
