package com.athaydes.osgiaas.javac.internal;

import com.athaydes.osgiaas.javac.JavaSnippet;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Representation of a Java class wrapping a {@link JavaSnippet}.
 */
public class SnippetClass {
    @SuppressWarnings( "FieldCanBeLocal" )
    private static final AtomicLong classCount = new AtomicLong( 0L );

    private final String className;
    private final String code;

    private SnippetClass( String className, String code ) {
        this.code = code;
        this.className = className;
    }

    /**
     * @return Java class' name
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return Java class source code
     */
    public String getCode() {
        return code;
    }

    /**
     * Wraps the provided Java source code snippet in a Java Class.
     *
     * @param snippet to be wrapped
     * @return a {@link SnippetClass}
     */
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

    /**
     * Create an instance of the given type using its default constructor,
     * casting it to {@link Callable}.
     *
     * @param type of instance to create
     * @return instance of {@link Callable} if the given type is a sub-type of {@link Callable}
     * and contains a public, default constructor.
     * @throws RuntimeException if the given type is not a sub-type of {@link Callable} or does not
     *                          have a public, default constructor.
     */
    public static Callable<?> uncheckedInstantiator( Class<?> type ) {
        try {
            return Callable.class.cast( type.newInstance() );
        } catch ( IllegalAccessException | InstantiationException e ) {
            throw new RuntimeException( e );
        }
    }

}
