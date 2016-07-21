package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.javac.ClassLoaderContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import java.util.Collection;
import java.util.Objects;

public class ClassLoaderCapabilities implements ClassLoaderContext {

    private Bundle bundle;

    public void activate( BundleContext context ) {
        this.bundle = context.getBundle();
    }

    public void deactivate( BundleContext context ) {
        this.bundle = null;
    }

    @Override
    public Collection<String> getClassesIn( String packageName ) {
        Objects.requireNonNull( bundle, "Did not set BundleContext" );
        String path = packageName.replace( ".", "/" );
        return bundle.adapt( BundleWiring.class )
                .listResources( path, "*.class", BundleWiring.LISTRESOURCES_LOCAL );
    }

    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

}
