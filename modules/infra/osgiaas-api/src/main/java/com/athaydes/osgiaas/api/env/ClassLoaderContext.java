package com.athaydes.osgiaas.api.env;

import java.io.InputStream;
import java.util.Collection;

/**
 * A ClassLoader context that can be used by the compiler to find and load classes.
 */
public interface ClassLoaderContext {

    /**
     * @return the ClassLoader to use to load classes within the compiler.
     */
    ClassLoader getClassLoader();

    /**
     * @param packageName package
     * @return all classes that the ClassLoader can load from the given package.
     */
    Collection<String> getClassesIn( String packageName );

    /**
     * @param resourcePath the resource name, normally obtained by calling {@link #getClassesIn(String)} and
     *                     turning that into a path.
     * @return the InputStream that can be used to read the resource contents
     * @throws RuntimeException if the class cannot be loaded by the ClassLoader.
     */
    default InputStream getInputStream( String resourcePath ) throws RuntimeException {
        InputStream inputStream = getClassLoader().getResourceAsStream( resourcePath );
        if ( inputStream == null ) {
            throw new RuntimeException( "Resource does not exist: " + resourcePath );
        } else {
            return inputStream;
        }
    }
}
