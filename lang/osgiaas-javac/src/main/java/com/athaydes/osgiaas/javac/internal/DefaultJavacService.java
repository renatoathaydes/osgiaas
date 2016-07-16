package com.athaydes.osgiaas.javac.internal;

import com.athaydes.osgiaas.javac.JavaSnippet;
import com.athaydes.osgiaas.javac.JavacService;
import net.openhft.compiler.CachedCompiler;
import net.openhft.compiler.CompilerUtils;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of JavacService.
 */
public class DefaultJavacService implements JavacService {

    @SuppressWarnings( "FieldCanBeLocal" )
    private static final AtomicLong classCount = new AtomicLong( 0L );

    private final CachedCompiler compiler;

    public DefaultJavacService() {
        this.compiler = CompilerUtils.CACHED_COMPILER;
    }

    @Override
    public Class compileJavaClass( ClassLoader classLoader,
                                   String qualifiedName,
                                   String code ) {
        return compileJavaClass( classLoader, qualifiedName, code, null );
    }

    @Override
    public Class compileJavaClass( ClassLoader classLoader,
                                   String qualifiedName,
                                   String code,
                                   /*Nullable*/ PrintWriter printWriter ) {
        try {
            return compiler.loadFromJava( classLoader, qualifiedName, code, printWriter );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Callable compileJavaSnippet( String snippet ) {
        return compileJavaSnippet(
                JavaSnippet.Builder.withCode( snippet ),
                getClass().getClassLoader() );
    }

    @Override
    public Callable compileJavaSnippet( String snippet, ClassLoader classLoader ) {
        return compileJavaSnippet(
                JavaSnippet.Builder.withCode( snippet ),
                classLoader );
    }

    @Override
    public Callable compileJavaSnippet( String snippet, ClassLoader classLoader, PrintWriter writer ) {
        return compileJavaSnippet(
                JavaSnippet.Builder.withCode( snippet ),
                classLoader, writer );
    }

    @Override
    public Callable compileJavaSnippet( JavaSnippet snippet ) {
        return compileJavaSnippet( snippet, getClass().getClassLoader(), null );
    }

    @Override
    public Callable compileJavaSnippet( JavaSnippet snippet, ClassLoader classLoader ) {
        return compileJavaSnippet( snippet, classLoader, null );
    }

    @Override
    public Callable compileJavaSnippet( JavaSnippet snippet, ClassLoader classLoader,
                                        /*Nullable*/ PrintWriter writer ) {
        SnippetClass snippetClass = asCallableSnippet( snippet );

        try {
            return ( Callable ) compileJavaClass(
                    classLoader, snippetClass.className, snippetClass.code, writer
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

        return new SnippetClass( className, importStatements +
                "public class " + className + " implements Callable {\n" +
                "public Object call() throws Exception {\n" +
                "" + snippet.getExecutableCode() + "\n" +
                "}\n" +
                "}" );
    }

    @Override
    public String getJavaSnippetClass( String snippet ) {
        return getJavaSnippetClass( JavaSnippet.Builder.withCode( snippet ) );
    }

    @Override
    public String getJavaSnippetClass( JavaSnippet snippet ) {
        return asCallableSnippet( snippet ).code;
    }

    private static class SnippetClass {
        final String className;
        final String code;

        SnippetClass( String className, String code ) {
            this.code = code;
            this.className = className;
        }
    }

}
