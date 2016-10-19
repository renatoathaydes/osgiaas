package com.athaydes.osgiaas.api.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class that can help handle multiple services.
 * <p>
 * To use this functionality, subclass this class, call {@link #addService(Object)}
 * and {@link #removeService(Object)} from your {@code bind} and {@code unbind} methods,
 * respectively, then simply call {@link #getServices()} when you need to use the services.
 * <p>
 * A new List is provided by {@code getServices()} on each invocation to avoid concurrency
 * issues.
 * <p>
 * To provide an ordering between the services, override the {@link #getComparator()} method.
 * <p>
 * This class is Thread-safe.
 */
public abstract class HasManyServices<ServiceType> {

    private final AtomicReference<List<ServiceType>> services =
            new AtomicReference<>( Collections.emptyList() );

    /**
     * @return a comparator to sort services.
     * <p>
     * The default comparator maintains items in insertion order.
     */
    protected Comparator<ServiceType> getComparator() {
        return ( a, b ) -> 0;
    }

    /**
     * Receive a service instance.
     *
     * @param service to add
     */
    protected void addService( ServiceType service ) {
        services.updateAndGet( old -> {
            List<ServiceType> update = new ArrayList<>( old );
            update.add( service );
            Collections.sort( update, getComparator() );
            return update;
        } );
    }

    /**
     * Remove a service.
     *
     * @param service to remove
     */
    protected void removeService( ServiceType service ) {
        services.updateAndGet( old -> {
            List<ServiceType> update = new ArrayList<>( old );
            update.remove( service );
            return update;
        } );
    }

    /**
     * @return a copy of the List of services currently available.
     */
    protected List<ServiceType> getServices() {
        return services.get();
    }

}
