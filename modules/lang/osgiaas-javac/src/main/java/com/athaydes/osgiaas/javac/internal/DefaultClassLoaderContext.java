package com.athaydes.osgiaas.javac.internal;


import com.athaydes.osgiaas.api.env.ClassLoaderContext;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public enum DefaultClassLoaderContext implements ClassLoaderContext {

    INSTANCE;

    private final ClassLoader loader = getClass().getClassLoader();

    @Override
    public ClassLoader getClassLoader() {
        return loader;
    }

    @Override
    public Collection<String> getClassesIn( String packageName ) {
        return Collections.emptyList();
    }

    @Override
    public InputStream getInputStream( String resourcePath ) throws RuntimeException {
        throw new RuntimeException( "The DefaultClassLoaderContext does not support getInputStream()" );
    }

}
