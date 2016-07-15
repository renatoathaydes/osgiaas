package com.athaydes.osgiaas.javac;

import com.athaydes.osgiaas.javac.internal.DefaultJavacService;

import java.util.concurrent.Callable;

/**
 * Java Compiler Service.
 */
public interface JavacService {

    Class compileJavaClass( ClassLoader classLoader,
                            String qualifiedName,
                            String code );

    Callable compileJavaSnippet( String snippet );

    Callable compileJavaSnippet( String snippet, ClassLoader classLoader );

    Callable compileJavaSnippet( JavaSnippet snippet );

    Callable compileJavaSnippet( JavaSnippet snippet, ClassLoader classLoader );

    String getJavaSnippetClass( String snippet );

    String getJavaSnippetClass( JavaSnippet snippet );

    /**
     * @return a default implementation of the JavacService.
     */
    static JavacService createDefault() {
        return new DefaultJavacService();
    }

}
