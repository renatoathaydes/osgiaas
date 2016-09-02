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
 */
public interface JavacService {

    default PrintStream defaultWriter() {
        return System.err;
    }

    default <T> Optional<Class<T>> compileJavaClass( ClassLoaderContext classLoaderContext,
                                                     String qualifiedName,
                                                     String code ) {
        return compileJavaClass( classLoaderContext, qualifiedName, code, defaultWriter() );
    }

    <T> Optional<Class<T>> compileJavaClass( ClassLoaderContext classLoaderContextContext,
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
     * @return a default implementation of the JavacService.
     */
    static JavacService createDefault() {
        return new OsgiaasJavaCompilerService();
    }

}
