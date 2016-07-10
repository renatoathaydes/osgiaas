package com.athaydes.osgiaas.javac;

import net.openhft.compiler.CompilerUtils;

import java.util.Collection;
import java.util.Collections;
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

    private static final String packageName = "snippet";

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

    public Callable compileJavaSnippet( String javaSnippet ) {
        return compileJavaSnippet( javaSnippet, Collections.emptySet() );
    }

    public Callable compileJavaSnippet( String javaSnippet, Collection<String> imports ) {
        return compileJavaSnippet( javaSnippet, imports, getClass().getClassLoader() );
    }

    public Callable compileJavaSnippet( String javaSnippet,
                                        Collection<String> imports,
                                        ClassLoader classLoader ) {
        Snippet snippet = asCallableSnippet( javaSnippet, imports );

        try {
            return ( Callable ) compileJavaClass(
                    classLoader, snippet.qualifiedName, snippet.code
            ).newInstance();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private static Snippet asCallableSnippet( String snippet, Collection<String> imports ) {
        String className = "JavaSnippet" + classCount.getAndIncrement();

        Set<String> importSet = new HashSet<>( imports );
        importSet.add( "java.util.concurrent.Callable" );

        String importStatements = importSet.stream()
                .map( it -> "import " + it + ";\n" )
                .reduce( ( a, b ) -> a + b ).orElse( "" );

        return new Snippet( className, "package " + packageName + ";\n" +
                importStatements +
                "public class " + className + " implements Callable {\n" +
                "public Object call() throws Exception {\n" +
                "" + snippet +
                "\n}\n" +
                "}" );
    }

    public String getJavaSnippetClass( String snippet, Collection<String> imports ) {
        return asCallableSnippet( snippet, imports ).code;
    }

    public static void main( String[] args ) throws Exception {
        new JavacService().compileJavaSnippet( "System.out.println(\"Hello, World!\")" ).call();
    }

    private static class Snippet {
        final String qualifiedName;
        final String className;
        final String code;

        public Snippet( String className, String code ) {
            this.code = code;
            this.className = className;
            this.qualifiedName = packageName + "." + className;
        }
    }

}
