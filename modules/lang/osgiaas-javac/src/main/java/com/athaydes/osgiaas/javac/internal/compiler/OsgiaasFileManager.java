package com.athaydes.osgiaas.javac.internal.compiler;

import com.athaydes.osgiaas.javac.internal.CompilerUtils;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class OsgiaasFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final OsgiaasClassLoader classLoader;
    private final Map<URI, JavaFileObject> fileObjectByURI = new HashMap<URI, JavaFileObject>();

    OsgiaasFileManager( JavaFileManager fileManager, OsgiaasClassLoader classLoader ) {
        super( fileManager );
        this.classLoader = classLoader;
    }

    @Override
    public FileObject getFileForInput( Location location, String packageName,
                                       String relativeName ) throws IOException {
        FileObject o = fileObjectByURI.get( CompilerUtils.uri( location, packageName, relativeName ) );
        if ( o != null )
            return o;
        return super.getFileForInput( location, packageName, relativeName );
    }

    void putFileForInput( StandardLocation location, String packageName,
                          String relativeName, JavaFileObject file ) {
        fileObjectByURI.put( CompilerUtils.uri( location, packageName, relativeName ), file );
    }

    void removeFile( StandardLocation location, String packageName, String relativeName ) {
        fileObjectByURI.remove( CompilerUtils.uri( location, packageName, relativeName ) );
    }

    @Override
    public JavaFileObject getJavaFileForOutput( Location location, String qualifiedName,
                                                Kind kind, FileObject outputFile ) throws IOException {
        OsgiaasFileObject file = new OsgiaasFileObject( qualifiedName, kind );
        classLoader.add( qualifiedName, file );
        return file;
    }

    @Override
    public ClassLoader getClassLoader( JavaFileManager.Location location ) {
        return classLoader;
    }

    @Override
    public String inferBinaryName( Location loc, JavaFileObject file ) {
        return ( file instanceof OsgiaasFileObject ?
                file.getName() :
                super.inferBinaryName( loc, file ) );
    }

    @Override
    public Iterable<JavaFileObject> list( Location location, String packageName,
                                          Set<Kind> kinds, boolean recurse ) throws IOException {
        Iterable<JavaFileObject> result = super.list( location, packageName, kinds,
                recurse );
        ArrayList<JavaFileObject> files = new ArrayList<>();
        if ( location == StandardLocation.CLASS_PATH
                && kinds.contains( Kind.CLASS ) ) {
            Collection<JavaFileObject> loaderClasses = classLoader.filesIn( packageName );
            files.addAll( loaderClasses );
            for (JavaFileObject file : fileObjectByURI.values()) {
                if ( file.getKind() == Kind.CLASS && file.getName().startsWith( packageName ) )
                    files.add( file );
            }
        } else if ( location == StandardLocation.SOURCE_PATH
                && kinds.contains( Kind.SOURCE ) ) {
            for (JavaFileObject file : fileObjectByURI.values()) {
                if ( file.getKind() == Kind.SOURCE && file.getName().startsWith( packageName ) )
                    files.add( file );
            }
        }
        for (JavaFileObject file : result) {
            files.add( file );
        }
        return files;
    }
}
