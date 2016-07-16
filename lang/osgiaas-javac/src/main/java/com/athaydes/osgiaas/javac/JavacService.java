package com.athaydes.osgiaas.javac;

import com.athaydes.osgiaas.javac.internal.DefaultJavacService;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Java Compiler Service.
 */
public interface JavacService {

    Class compileJavaClass( ClassLoader classLoader,
                            String qualifiedName,
                            String code );

    Class compileJavaClass( ClassLoader classLoader,
                            String qualifiedName,
                            String code,
                            PrintWriter writer );

    Callable compileJavaSnippet( String snippet );

    Callable compileJavaSnippet( String snippet, ClassLoader classLoader );

    Callable compileJavaSnippet( String snippet, ClassLoader classLoader, PrintWriter writer );

    Callable compileJavaSnippet( JavaSnippet snippet );

    Callable compileJavaSnippet( JavaSnippet snippet, ClassLoader classLoader );

    Callable compileJavaSnippet( JavaSnippet snippet, ClassLoader classLoader, PrintWriter writer );

    String getJavaSnippetClass( String snippet );

    String getJavaSnippetClass( JavaSnippet snippet );

    /**
     * @return a default implementation of the JavacService.
     */
    static JavacService createDefault() {
        return new DefaultJavacService();
    }

}
