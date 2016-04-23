package com.athaydes.osgiaas.api.service;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Util class to make it easy to handle dynamic services.
 */
public class DynamicServiceHelper {

    public static <T> void with( AtomicReference<T> reference, Consumer<T> consumer ) {
        with( reference, consumer, () -> {
        } );
    }

    public static <T> void with( AtomicReference<T> reference,
                                 Consumer<T> consumer,
                                 Runnable onUnavailable ) {
        @Nullable
        T instance = reference.get();
        if ( instance != null ) {
            consumer.accept( instance );
        } else {
            onUnavailable.run();
        }
    }

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
