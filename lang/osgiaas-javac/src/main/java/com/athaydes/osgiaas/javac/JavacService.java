package com.athaydes.osgiaas.javac;

import net.openhft.compiler.CompilerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

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
        return compileJavaSnippet( javaSnippet, getClass().getClassLoader() );
    }

    public Callable compileJavaSnippet( String javaSnippet, ClassLoader classLoader ) {
        javaSnippet = javaSnippet.trim();
        if ( !javaSnippet.endsWith( ";" ) ) {
            javaSnippet = javaSnippet + ";";
        }

        List<Snippet> snippets = new ArrayList<>( 2 );

        if ( !javaSnippet.startsWith( "return " ) ) {
            snippets.add( asCallableSnippet( "return " + javaSnippet ) );
            snippets.add( asCallableSnippet( javaSnippet + "return null;" ) );
        }

        snippets.add( asCallableSnippet( javaSnippet ) );

        try {
            return ( Callable ) tryInTurn( snippets, snippet ->
                    compileJavaClass( classLoader, snippet.qualifiedName, snippet.code ) )
                    .newInstance();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private static Snippet asCallableSnippet( String snippet ) {

        String className = "JavaSnippet" + classCount.getAndIncrement();

        return new Snippet( className, "package " + packageName + ";" +
                "import java.util.concurrent.Callable;" +
                "public class " + className + " implements Callable {" +
                "public Object call() throws Exception {\n" +
                "" + snippet +
                "}" +
                "}" );
    }

    private static Class tryInTurn( List<Snippet> candidateSnippets,
                                    Function<Snippet, Class> snippetCompiler )
            throws Exception {
        Exception error = null;

        for (Snippet snippet : candidateSnippets) {
            try {
                return snippetCompiler.apply( snippet );
            } catch ( Exception e ) {
                if ( error == null ) {
                    error = e;
                }
            }
        }

        throw error;
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
