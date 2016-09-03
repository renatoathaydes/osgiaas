package com.athaydes.osgiaas.javac.internal;

import com.athaydes.osgiaas.javac.JavaSnippet;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class SnippetClass {
    @SuppressWarnings( "FieldCanBeLocal" )
    private static final AtomicLong classCount = new AtomicLong( 0L );

    private final String className;
    private final String code;

    SnippetClass( String className, String code ) {
        this.code = code;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public String getCode() {
        return code;
    }

    public static SnippetClass asCallableSnippet( JavaSnippet snippet ) {
        String className = "JavaSnippet" + classCount.getAndIncrement();

        Set<String> importSet = new HashSet<>( snippet.getImports() );
        importSet.add( "java.util.concurrent.Callable" );

        String importStatements = importSet.stream()
                .map( it -> "import " + it + ";\n" )
                .reduce( ( a, b ) -> a + b ).orElse( "" );

        return new SnippetClass( className, importStatements +
                "public class " + className + " implements Callable {\n" +
                "public Object call() throws Exception {\n" +
                "" + snippet.getExecutableCode() +
                "}\n" +
                "}" );
    }

    public static Callable<?> uncheckedInstantiator( Class<?> type ) {
        try {
            return Callable.class.cast( type.newInstance() );
        } catch ( IllegalAccessException | InstantiationException e ) {
            throw new RuntimeException( e );
        }
    }

}
