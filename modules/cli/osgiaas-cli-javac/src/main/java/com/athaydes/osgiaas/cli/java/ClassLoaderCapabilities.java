package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.javac.ClassLoaderContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return Stream.of( bundle.getBundleContext().getBundles() )
                .filter( it -> exportsPackage( packageName, it ) )
                .map( it -> it.adapt( BundleWiring.class ) )
                .filter( it -> it != null )
                .flatMap( it -> it.listResources( path, "*.class", BundleWiring.LISTRESOURCES_LOCAL ).stream() )
                .filter( it -> it != null && !it.contains( "$" ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    private static boolean exportsPackage( String packageName, Bundle bundle ) {
        @Nullable String exports = bundle.getHeaders().get( "Export-Package" );
        return exports != null &&
                extractPackageNames( exports ).contains( packageName );
    }

    private static Set<String> extractPackageNames( String exportDeclaration ) {
        return Stream.of( exportDeclaration.split( "," ) )
                .map( it -> it.split( ";" )[ 0 ] )
                .collect( Collectors.toSet() );
    }

}
