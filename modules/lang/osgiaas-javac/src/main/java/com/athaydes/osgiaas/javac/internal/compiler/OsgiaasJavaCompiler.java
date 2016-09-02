package com.athaydes.osgiaas.javac.internal.compiler;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.javac.internal.CompilerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SuppressWarnings( "WeakerAccess" )
public class OsgiaasJavaCompiler {

    private static final Logger logger = LoggerFactory.getLogger( OsgiaasJavaCompiler.class );

    private final OsgiaasClassLoader classLoader;
    private final JavaCompiler compiler;
    private final List<String> options;
    private DiagnosticCollector<JavaFileObject> diagnostics;
    private final OsgiaasFileManager javaFileManager;

    @SuppressWarnings( "WeakerAccess" )
    public OsgiaasJavaCompiler( ClassLoaderContext classLoaderContext ) {
        this( classLoaderContext, Collections.emptyList() );
    }

    @SuppressWarnings( "WeakerAccess" )
    public OsgiaasJavaCompiler( ClassLoaderContext classLoaderContext, Iterable<String> options ) {
        compiler = ToolProvider.getSystemJavaCompiler();

        if ( compiler == null ) {
            logger.error( "The Java compiler is not present in the classpath" );
            throw new IllegalStateException( "Cannot find the system Java compiler. "
                    + "Check that your class path includes tools.jar" );
        }

        classLoader = new OsgiaasClassLoader( classLoaderContext );
        diagnostics = new DiagnosticCollector<>();

        javaFileManager = new OsgiaasFileManager(
                compiler.getStandardFileManager( diagnostics, null, null ),
                classLoader );

        this.options = new ArrayList<>();
        if ( options != null ) {
            options.forEach( this.options::add );
        }
    }

    <T> Optional<Class<T>> compile( String qualifiedClassName,
                                    CharSequence javaSource,
                                    PrintStream writer ) {
        logger.info( "Compiling {} from source code", qualifiedClassName );
        String className = CompilerUtils.simpleClassNameFrom( qualifiedClassName );
        String packageName = CompilerUtils.packageOf( qualifiedClassName );

        OsgiaasFileObject source = new OsgiaasFileObject( className, javaSource );

        PrintWriter errorWriter = new PrintWriter( writer );

        boolean ok = false;

        try {
            javaFileManager.putFileForInput( StandardLocation.SOURCE_PATH, packageName,
                    className + CompilerUtils.JAVA_EXTENSION, source );

            JavaCompiler.CompilationTask task = compiler.getTask(
                    errorWriter, javaFileManager, diagnostics,
                    options, null, Collections.singleton( source ) );

            ok = task.call();
        } finally {
            if ( !ok ) {
                javaFileManager.removeFile( StandardLocation.SOURCE_PATH, packageName,
                        className + CompilerUtils.JAVA_EXTENSION );
            }
            diagnostics.getDiagnostics().forEach( errorWriter::println );
            diagnostics = new DiagnosticCollector<>();
            errorWriter.flush();
        }

        if ( ok ) try {
            return Optional.of( loadClass( qualifiedClassName ) );
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeException( e );
        }
        else {
            return Optional.empty();
        }
    }

    @SuppressWarnings( "unchecked" )
    private <T> Class<T> loadClass( String qualifiedClassName )
            throws ClassNotFoundException {
        return ( Class<T> ) classLoader.loadClass( qualifiedClassName, true );
    }

}

