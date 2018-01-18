package com.athaydes.osgiaas.javac;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.javac.internal.DefaultClassLoaderContext;
import com.athaydes.osgiaas.javac.internal.SnippetClass;
import com.athaydes.osgiaas.javac.internal.compiler.OsgiaasJavaCompilerService;

import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.athaydes.osgiaas.javac.internal.SnippetClass.asCallableSnippet;

/**
 * Java Compiler Service.
 * <p>
 * This service uses a {@link com.athaydes.osgiaas.javac.internal.compiler.OsgiaasJavaCompiler} to compile Java
 * source code, managing different class loaders as necessary.
 * <p>
 * Unlike the compiler, this service can compile both Java classes and Java source code snippets
 * (by first wrapping them into a simple Java class with a main method).
 */
public interface JavacService {

    /**
     * @return the default writer, System.err.
     */
    default PrintStream defaultWriter() {
        return System.err;
    }

    /**
     * Compiles a Java class with the given name.
     *
     * @param classLoaderContext the ClassLoader context
     * @param qualifiedName      qualified name of the Java class
     * @param code               the Java class source code
     * @param <T>                type of the compiled class (usually Object or an interface implemented by the class)
     * @return the compiled class Object if successful, or empty if a compilation error occurs.
     * Compilation errors are written to the default writer.
     */
    default <T> Optional<Class<T>> compileJavaClass( ClassLoaderContext classLoaderContext,
                                                     String qualifiedName,
                                                     String code ) {
        return compileJavaClass( classLoaderContext, qualifiedName, code, defaultWriter() );
    }

    /**
     * @param classLoaderContext the ClassLoader context
     * @param qualifiedName      qualified name of the Java class
     * @param code               the Java class source code
     * @param writer             to capture the compiler output
     * @param <T>                type of the compiled class (usually Object or an interface implemented by the class)
     * @return the compiled class Object if successful, or empty if a compilation error occurs.
     * Compilation errors are written to the provided writer.
     */
    <T> Optional<Class<T>> compileJavaClass( ClassLoaderContext classLoaderContext,
                                             String qualifiedName,
                                             String code,
                                             PrintStream writer );

    default Optional<Callable<?>> compileJavaSnippet( String snippet ) {
        return compileJavaSnippet( JavaSnippet.Builder.withCode( snippet ),
                DefaultClassLoaderContext.INSTANCE, defaultWriter() );
    }

    default Optional<Callable<?>> compileJavaSnippet( String snippet, ClassLoaderContext classLoaderContext ) {
        return compileJavaSnippet( JavaSnippet.Builder.withCode( snippet ),
                classLoaderContext, defaultWriter() );
    }

    default Optional<Callable<?>> compileJavaSnippet( String snippet,
                                                      ClassLoaderContext classLoaderContext,
                                                      PrintStream writer ) {
        return compileJavaSnippet( JavaSnippet.Builder.withCode( snippet ), classLoaderContext, writer );
    }

    default Optional<Callable<?>> compileJavaSnippet( JavaSnippet snippet ) {
        return compileJavaSnippet( snippet, DefaultClassLoaderContext.INSTANCE, defaultWriter() );
    }

    default Optional<Callable<?>> compileJavaSnippet( JavaSnippet snippet, ClassLoaderContext classLoaderContext ) {
        return compileJavaSnippet( snippet, classLoaderContext, defaultWriter() );
    }

    default Optional<Callable<?>> compileJavaSnippet( JavaSnippet snippet,
                                                      ClassLoaderContext classLoaderContext,
                                                      PrintStream writer ) {
        SnippetClass snippetClass = asCallableSnippet( snippet );
        try {
            return compileJavaClass(
                    classLoaderContext, snippetClass.getClassName(), snippetClass.getCode(), writer
            ).map( ( SnippetClass::uncheckedInstantiator ) );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    default String getJavaSnippetClass( JavaSnippet snippet ) {
        return asCallableSnippet( snippet ).getCode();
    }

    /**
     * @param classLoaderContext to be augmented
     * @return the provided classLoader context augmented with the Java compiler's
     * own loader, which allows the augmented context to see classes compiled by the
     * JavaC compiler as well as the ones from the provided context.
     */
    ClassLoaderContext getAugmentedClassLoaderContext( ClassLoaderContext classLoaderContext );

    /**
     * @return a default implementation of the JavacService.
     */
    static JavacService createDefault() {
        return new OsgiaasJavaCompilerService();
    }

}
