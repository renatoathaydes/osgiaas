package com.athaydes.osgiaas.javac;

import net.openhft.compiler.CompilerUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Java Compiler Service.
 */
public class JavacService {

    @SuppressWarnings( "FieldCanBeLocal" )
    private static final AtomicLong classCount = new AtomicLong( 0L );

    public Class compileJavaClass( ClassLoader classLoader,
                                   String qualifiedName,
                                   String code ) {
        try {
            return CompilerUtils.CACHED_COMPILER
                    .loadFromJava( classLoader, qualifiedName, code );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public Callable compileJavaSnippet( String snippet, ClassLoader classLoader ) {
        return compileJavaSnippet(
                JavaSnippet.Builder.withCode( snippet ),
                classLoader );
    }

    public Callable compileJavaSnippet( String snippet ) {
        return compileJavaSnippet(
                JavaSnippet.Builder.withCode( snippet ),
                getClass().getClassLoader() );
    }

    public Callable compileJavaSnippet( JavaSnippet snippet ) {
        return compileJavaSnippet( snippet, getClass().getClassLoader() );
    }

    public Callable compileJavaSnippet( JavaSnippet snippet, ClassLoader classLoader ) {
        SnippetClass snippetClass = asCallableSnippet( snippet );

        try {
            return ( Callable ) compileJavaClass(
                    classLoader, snippetClass.className, snippetClass.code
            ).newInstance();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private static SnippetClass asCallableSnippet( JavaSnippet snippet ) {
        String className = "JavaSnippet" + classCount.getAndIncrement();

        Set<String> importSet = new HashSet<>( snippet.getImports() );
        importSet.add( "java.util.concurrent.Callable" );

        String importStatements = importSet.stream()
                .map( it -> "import " + it + ";\n" )
                .reduce( ( a, b ) -> a + b ).orElse( "" );

        String classes = snippet.getClassDefinitions().stream()
                .map( it -> it + "\n" )
                .reduce( ( a, b ) -> a + b ).orElse( "" );

        return new SnippetClass( className, importStatements +
                ( classes.isEmpty() ? "" : "\n" + classes ) +
                "public class " + className + " implements Callable {\n" +
                "public Object call() throws Exception {\n" +
                "" + snippet.getExecutableCode() + "\n" +
                "}\n" +
                "}" );
    }

    public String getJavaSnippetClass( String snippet ) {
        return getJavaSnippetClass( JavaSnippet.Builder.withCode( snippet ) );
    }

    public String getJavaSnippetClass( JavaSnippet snippet ) {
        return asCallableSnippet( snippet ).code;
    }

    private static class SnippetClass {
        final String className;
        final String code;

        public SnippetClass( String className, String code ) {
            this.code = code;
            this.className = className;
        }
    }

}
