package com.athaydes.osgiaas.cli.util;

import com.athaydes.osgiaas.api.cli.Cli;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A little base class for classes that use the Cli service.
 */
public class UsesCli {

    private final AtomicReference<Cli> cli = new AtomicReference<>();

    public void setCli( Cli cli ) {
        this.cli.set( cli );
    }

    public void removeCli( Cli cli ) {
        this.cli.set( null );
    }

    protected void withCli( Consumer<Cli> consumer ) {
        DynamicServiceHelper.with( cli, consumer );
    }

}
