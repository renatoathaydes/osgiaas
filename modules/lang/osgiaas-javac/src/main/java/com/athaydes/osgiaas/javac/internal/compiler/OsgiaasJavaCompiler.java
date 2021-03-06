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

/**
 * A Java compiler that compiles classes in-memory.
 * <p>
 * If you want to use several classloaders to compile different classes, consider using
 * one of the {@link com.athaydes.osgiaas.javac.JavacService} implementations.
 */
@SuppressWarnings( "WeakerAccess" )
public class OsgiaasJavaCompiler {

    private static final Logger logger = LoggerFactory.getLogger( OsgiaasJavaCompiler.class );

    private final OsgiaasClassLoader classLoader;
    private final JavaCompiler compiler;
    private final List<String> options;
    private DiagnosticCollector<JavaFileObject> diagnostics;
    private final OsgiaasFileManager javaFileManager;

    /**
     * Create an instance of {@link OsgiaasJavaCompiler}.
     *
     * @param classLoaderContext ClassLoader context
     */
    @SuppressWarnings( "WeakerAccess" )
    public OsgiaasJavaCompiler( ClassLoaderContext classLoaderContext ) {
        this( classLoaderContext, Collections.emptyList() );
    }

    /**
     * Create an instance of {@link OsgiaasJavaCompiler}.
     *
     * @param classLoaderContext ClassLoader context
     * @param options            javac options (see "javac -help" for details)
     */
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

    /**
     * @return the {@link ClassLoaderContext} used by this compiler.
     */
    public ClassLoaderContext getClassLoaderContext() {
        return classLoader;
    }

    /**
     * Compile a class with the given name and source.
     * <p>
     * The output of the compiler is written to the provided writer.
     *
     * @param qualifiedClassName the qualified name of the class
     * @param javaSource         the Java class source code
     * @param writer             to capture the compiler output
     * @param <T>                type of the compiled class (usually Object or an interface implemented by the class)
     * @return the compiled class Object if successful, or empty if a compilation error occurs.
     * Compilation errors are written to the provided writer.
     */
    public <T> Optional<Class<T>> compile( String qualifiedClassName,
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

