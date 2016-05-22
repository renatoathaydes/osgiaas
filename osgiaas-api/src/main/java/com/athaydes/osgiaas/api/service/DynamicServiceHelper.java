package com.athaydes.osgiaas.api.service;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class to make it easy to handle dynamic services.
 * <p>
 * All methods of this class are Thread-safe.
 */
public class DynamicServiceHelper {

    /**
     * Given a reference to a service, call the provided consumer if the service
     * is present, or do nothing otherwise.
     *
     * @param reference to a service
     * @param consumer  consumer of the service
     * @param <T>       type of the service
     * @see #with(AtomicReference, Consumer, Runnable)
     */
    public static <T> void with( AtomicReference<T> reference, Consumer<T> consumer ) {
        with( reference, consumer, () -> {
        } );
    }


    /**
     * Given a reference to a service, call the provided consumer if the service
     * is present, or call the onUnavailable callback if the service is unavailable.
     *
     * @param reference     to a service
     * @param consumer      consumer of the service
     * @param onUnavailable callback to call if the service is unavailable
     * @param <T>           type of the service
     * @see #with(AtomicReference, Consumer)
     */
    public static <T> void with( AtomicReference<T> reference,
                                 Consumer<T> consumer,
                                 Runnable onUnavailable ) {
        T instance = reference.get();
        if ( instance != null ) {
            consumer.accept( instance );
        } else {
            onUnavailable.run();
        }
    }


    /**
     * Given a reference to a service, call the provided consumer if the service
     * is present, or call the onUnavailable callback if the service is unavailable.
     *
     * @param reference       to a service
     * @param consumer        function used to provide a value given the service is present
     * @param defaultSupplier supplier of a default value to return in case the service is unavailable
     * @param <T>             type of the service
     * @param <R>             type of the return value
     * @return the result of calling the #consumer function in case the service is available,
     * or of calling the provided #defaultSupplier in case the service is unavailable
     */
    public static <T, R> R let( AtomicReference<T> reference,
                                Function<T, R> consumer,
                                Supplier<R> defaultSupplier ) {
        AtomicReference<R> resultRef = new AtomicReference<>();
        with( reference,
                ( value ) -> resultRef.set( consumer.apply( value ) ),
                () -> resultRef.set( defaultSupplier.get() ) );
        return resultRef.get();
    }

}
