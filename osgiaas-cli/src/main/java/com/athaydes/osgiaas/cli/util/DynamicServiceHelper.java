package com.athaydes.osgiaas.cli.util;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

}
