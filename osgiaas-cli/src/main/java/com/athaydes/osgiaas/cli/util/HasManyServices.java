package com.athaydes.osgiaas.cli.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class that can help handle multiple services.
 */
public class HasManyServices<ServiceType> {

    private final AtomicReference<Set<ServiceType>> services =
            new AtomicReference<>( new HashSet<>() );

    public void addService( ServiceType service ) {
        services.updateAndGet( old -> {
            old.add( service );
            return new HashSet<>( old );
        } );
    }

    public void removeService( ServiceType service ) {
        services.updateAndGet( old -> {
            old.remove( service );
            return new HashSet<>( old );
        } );
    }

    public Set<ServiceType> getServices() {
        return services.get();
    }

}
