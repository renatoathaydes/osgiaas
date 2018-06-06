package com.athaydes.osgiaas.javac.internal.compiler;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.athaydes.osgiaas.javac.internal.CompilerUtils.classNameFromPath;
import static com.athaydes.osgiaas.javac.internal.CompilerUtils.packageOf;

/**
 * Implementation of {@link ClassLoaderContext} for use within OSGi.
 */
final class OsgiaasClassLoader extends ClassLoader
        implements ClassLoaderContext {

    private static final Logger logger = LoggerFactory.getLogger( OsgiaasClassLoader.class );

    private final Map<String, OsgiaasFileObject> fileByClassName = new HashMap<>();
    private final Map<String, Collection<JavaFileObject>> filesByPackage = new HashMap<>();
    private final ClassLoaderContext classLoaderContext;

    OsgiaasClassLoader( final ClassLoaderContext classLoaderContext ) {
        super( classLoaderContext.getClassLoader() );
        this.classLoaderContext = classLoaderContext;
    }

    Collection<JavaFileObject> filesIn( String packageName ) {
        Stream<JavaFileObject> compiledClasses = fileByClassName.entrySet().stream()
                .filter( entry -> packageOf( entry.getKey() ).equals( packageName ) )
                .map( Map.Entry::getValue );

        Collection<JavaFileObject> classLoaderClasses = filesByPackage
                .computeIfAbsent( packageName, this::computeClassesIn );

        return Stream.concat( compiledClasses, classLoaderClasses.stream() )
                .collect( Collectors.toSet() );
    }

    private Collection<JavaFileObject> computeClassesIn( String packageName ) {
        logger.debug( "Computing classes in package {}", packageName );
        return classLoaderContext.getClassesIn( packageName ).stream()
                .map( path -> loadBytecodeFrom(
                        new OsgiaasFileObject( classNameFromPath( path ), JavaFileObject.Kind.CLASS ),
                        classLoaderContext.getInputStream( path ) ) )
                .collect( Collectors.toList() );
    }

    private static JavaFileObject loadBytecodeFrom( OsgiaasFileObject file, InputStream input ) {
        byte[] buffer = new byte[ 4096 ];
        int len;

        try ( OutputStream out = file.openOutputStream() ) {
            while ( ( len = input.read( buffer ) ) != -1 ) {
                out.write( buffer, 0, len );
            }
        } catch ( IOException e ) {
            logger.warn( "Problem loading bytecode for {}: {}", file, e );
            throw new RuntimeException( e );
        } finally {
            try {
                input.close();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }

        return file;
    }

    // expose publicly
    @Override
    public Class<?> loadClass( String name, boolean resolve ) throws ClassNotFoundException {
        try {
            return super.loadClass( name, resolve );
        } catch ( ClassNotFoundException e ) {
            return classLoaderContext.getClassLoader().loadClass( name );
        }
    }

    @Override
    protected Class<?> findClass( String qualifiedClassName )
            throws ClassNotFoundException {
        OsgiaasFileObject file = fileByClassName.get( qualifiedClassName );
        if ( file != null && file.getKind() == JavaFileObject.Kind.CLASS ) {
            byte[] bytes = file.getByteCode();
            return defineClass( qualifiedClassName, bytes, 0, bytes.length );
        }

        return super.findClass( qualifiedClassName );
    }

    void add( String qualifiedClassName, OsgiaasFileObject javaFile ) {
        fileByClassName.put( qualifiedClassName, javaFile );
    }

    @Override
    public InputStream getResourceAsStream( String name ) {
        if ( name.endsWith( ".class" ) ) {
            String qualifiedClassName = name.substring( 0,
                    name.length() - ".class".length() ).replace( '/', '.' );
            OsgiaasFileObject file = fileByClassName.get( qualifiedClassName );
            if ( file != null ) {
                return new ByteArrayInputStream( file.getByteCode() );
            }
        }

        InputStream inputStream = super.getResourceAsStream( name );

        if ( inputStream == null ) {
            inputStream = classLoaderContext.getClassLoader().getResourceAsStream( name );
        }

        return inputStream;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this;
    }

    @Override
    public Collection<String> getClassesIn( String packageName ) {
        Collection<String> classes = classLoaderContext.getClassesIn( packageName );
        Set<String> customClasses = fileByClassName.keySet();

        Set<String> result = new HashSet<>( classes.size() + customClasses.size() );
        result.addAll( classes );
        result.addAll( customClasses );

        return result;
    }

}
