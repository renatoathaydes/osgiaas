package com.athaydes.osgiaas.javac;

import net.openhft.compiler.CompilerUtils;

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
        return compileJavaSnippet( javaSnippet, getClass().getClassLoader() );
    }

    public Callable compileJavaSnippet( String javaSnippet, ClassLoader classLoader ) {
        Snippet snippet = asCallableSnippet( javaSnippet );

        try {
            return ( Callable ) compileJavaClass(
                    classLoader, snippet.qualifiedName, snippet.code
            ).newInstance();
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
                "\n}" +
                "}" );
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
