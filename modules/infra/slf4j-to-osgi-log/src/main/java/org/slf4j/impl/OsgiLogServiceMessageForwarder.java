package org.slf4j.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

class OsgiLogServiceMessageForwarder {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final ThreadLocal<MessageReceiver> logRunner = new ThreadLocal<MessageReceiver>() {
        @Override
        protected MessageReceiver initialValue() {
            return new NoLogServiceMessageReceiver();
        }
    };

    void receiveMessage( int level, String message, @Nullable Throwable throwable ) {
        executor.execute( () -> logRunner.get().receive( logService -> {
            if ( throwable == null ) {
                logService.log( level, message );
            } else {
                logService.log( level, message, throwable );
            }
        } ) );
    }

    private interface MessageReceiver {
        LinkedList<Consumer<LogService>> MESSAGE_BUFFER = new LinkedList<>();

        void receive( Consumer<LogService> loggingEvent );
    }

    private static final class NoLogServiceMessageReceiver implements MessageReceiver {
        private void init() {
            Optional.ofNullable( FrameworkUtil.getBundle( Logger.class ) )
                    .map( Bundle::getBundleContext )
                    .ifPresent( ( bundleContext -> {
                        ServiceTracker<LogService, ? extends LogService> tracker = new ServiceTracker<>(
                                bundleContext, LogService.class.getName(), null );
                        tracker.open();
                        logRunner.set( new InitializedMessageReceiver( tracker ) );
                    } ) );
        }

        @Override
        public void receive( Consumer<LogService> loggingEvent ) {
            MESSAGE_BUFFER.add( loggingEvent );
            init();
        }
    }

    private static final class InitializedMessageReceiver implements MessageReceiver {
        private final ServiceTracker<LogService, ? extends LogService> tracker;

        InitializedMessageReceiver( ServiceTracker<LogService, ? extends LogService> tracker ) {
            this.tracker = tracker;
            drainBuffer();
        }

        @Override
        public void receive( Consumer<LogService> loggingEvent ) {
            MESSAGE_BUFFER.add( loggingEvent );
            drainBuffer();
        }

        private void drainBuffer() {
            LogService logService = tracker.getService();
            if ( logService != null ) {
                while ( !MESSAGE_BUFFER.isEmpty() ) {
                    MESSAGE_BUFFER.removeFirst().accept( logService );
                }
            }
        }
    }

}
