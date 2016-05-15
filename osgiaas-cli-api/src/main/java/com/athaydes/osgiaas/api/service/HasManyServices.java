package com.athaydes.osgiaas.api.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class that can help handle multiple services.
 */
public abstract class HasManyServices<ServiceType> {

    private final AtomicReference<List<ServiceType>> services =
            new AtomicReference<>( Collections.emptyList() );

    protected abstract Comparator<ServiceType> getComparator();

    public void addService( ServiceType service ) {
        services.updateAndGet( old -> {
            List<ServiceType> update = new ArrayList<>( old );
            update.add( service );
            Collections.sort( update, getComparator() );
            return update;
        } );
    }

    public void removeService( ServiceType service ) {
        services.updateAndGet( old -> {
            List<ServiceType> update = new ArrayList<>( old );
            update.remove( service );
            return update;
        } );
    }

    public List<ServiceType> getServices() {
        return services.get();
    }

}
