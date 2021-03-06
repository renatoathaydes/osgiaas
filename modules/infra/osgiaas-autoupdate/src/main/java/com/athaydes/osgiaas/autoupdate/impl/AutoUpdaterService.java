package com.athaydes.osgiaas.autoupdate.impl;

import com.athaydes.osgiaas.autoupdate.AutoUpdater;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static com.athaydes.osgiaas.api.service.DynamicServiceHelper.with;

public class AutoUpdaterService implements BundleListener {

    private static final Logger log = LoggerFactory.getLogger( AutoUpdaterService.class );

    private final AtomicReference<AutoUpdater> autoUpdaterRef = new AtomicReference<>();
    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();
    private final Set<Long> pendingForRegistration = new ConcurrentSkipListSet<>();

    @Nullable
    private volatile AutoUpdateConfig config = AutoUpdateConfig.defaultConfig();

    public void activate( ComponentContext context, @Nullable Map<String, ?> properties ) {
        log.debug( "Service is active" );
        contextRef.set( context );

        if ( properties == null ) {
            log.info( "ConfigAdmin Service did not provide any configuration, " +
                    "using System properties and defaults instead." );
            config = AutoUpdateConfig.fromSystemProperties();
        } else {
            log.info( "Using ConfigAdmin-provided configuration for the service." );
            config = AutoUpdateConfig.fromMap( properties );
        }

        log.info( "Configuration received: {}", config );

        subscribeAllBundles();
    }

    public void deactivate( ComponentContext context ) {
        contextRef.set( null );
    }

    public void setAutoUpdater( AutoUpdater autoUpdater ) {
        autoUpdaterRef.set( autoUpdater );
        log.debug( "Received AutoUpdater instance: {}", autoUpdater );
        subscribeAllBundles();
    }

    public void unsetAutoUpdater( AutoUpdater autoUpdater ) {
        autoUpdaterRef.set( null );
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        long bundleId = event.getBundle().getBundleId();
        switch ( event.getType() ) {
            case BundleEvent.INSTALLED:
                subscribeBundle( bundleId );
                break;
            case BundleEvent.UNINSTALLED:
                pendingForRegistration.remove( bundleId );
                break;
        }
    }

    private void subscribeAllBundles() {
        @Nullable AutoUpdateConfig currentConfig = config;

        if ( currentConfig != null ) {
            usingServices(
                    ( context, autoUpdater ) -> {
                        log.info( "All services ready, subscribing bundles for auto-update" );
                        autoUpdater.subscribeAllBundles( currentConfig,
                                currentConfig.getBundleExcludes() );
                    },
                    () -> log.debug( "Unable to subscribe bundles for auto-update as " +
                            "not all required services are available" ) );
        } else {
            log.debug( "Skipping subscription of all bundles, config is not available yet." );
        }
    }

    private void subscribeBundle( long bundleId ) {
        @Nullable AutoUpdateConfig currentConfig = config;

        if ( currentConfig != null ) {
            usingServices( ( context, autoUpdater ) -> {
                autoUpdater.subscribeBundle( bundleId, currentConfig );
                Iterator<Long> bundleIdsToSubscribe = pendingForRegistration.iterator();
                while ( bundleIdsToSubscribe.hasNext() ) {
                    Long nextId = bundleIdsToSubscribe.next();
                    bundleIdsToSubscribe.remove();
                    autoUpdater.subscribeBundle( nextId, currentConfig );
                }
            }, () -> {
                log.info( "Unable to subscribe bundle [{}] for auto-update as " +
                        "not all required services are available." +
                        " Will attempt to register it later.", bundleId );
                pendingForRegistration.add( bundleId );
            } );
        } else {
            log.debug( "Skipping subscription of bundle with ID {}" +
                    ", config is not available yet. Will attempt to register it later.", bundleId );
            pendingForRegistration.add( bundleId );
        }
    }

    private void usingServices( BiConsumer<ComponentContext, AutoUpdater> consumer,
                                Runnable onUnavailable ) {
        new Thread( () -> {
            with( contextRef, context ->
                            with( autoUpdaterRef, updater ->
                                    consumer.accept( context, updater ), onUnavailable ),
                    onUnavailable );
        } ).start();
    }

}