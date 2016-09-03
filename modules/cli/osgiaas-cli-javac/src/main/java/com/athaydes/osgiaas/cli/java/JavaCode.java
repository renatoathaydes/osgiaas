package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext;
import com.athaydes.osgiaas.javac.JavaSnippet;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps state about the Java code to be executed in the shell.
 */
class JavaCode implements JavaSnippet, JavaAutocompleteContext {
    private final LinkedList<String> javaLines = new LinkedList<>();
    private final LinkedList<String> tempJavaLines = new LinkedList<>();

    private final Set<String> imports = new HashSet<>();
    private final Set<String> tempImports = new HashSet<>();
    private boolean addBindingsToCode = true;
    private final Supplier<Optional<ClassLoaderContext>> classLoaderContextSupplier;

    public JavaCode() {
        this( Optional::empty );
    }

    public JavaCode( Supplier<Optional<ClassLoaderContext>> classLoaderContextSupplier ) {
        this.classLoaderContextSupplier = classLoaderContextSupplier;
        resetCode();
        resetImports();
    }

    @Override
    public Optional<ClassLoaderContext> getClassLoaderContext() {
        return classLoaderContextSupplier.get();
    }

    void setAddBindingsToCode( boolean addBindingsToCode ) {
        this.addBindingsToCode = addBindingsToCode;
        resetCode();
    }

    void addLine( String line ) {
        if ( line.startsWith( "import " ) ) {
            tempImports.add( line.substring( "import ".length() ) );
        } else {
            tempJavaLines.add( line );
        }
    }

    Collection<String> getJavaLines() {
        return javaLines;
    }

    void resetCode() {
        tempJavaLines.clear();
        javaLines.clear();

        if ( addBindingsToCode ) {
            javaLines.add( "PrintStream out = Binding.out" );
            javaLines.add( "PrintStream err = Binding.err" );
            javaLines.add( "BundleContext ctx = Binding.ctx" );
        }
    }

    void resetImports() {
        tempImports.clear();
        imports.clear();
        imports.add( "com.athaydes.osgiaas.cli.java.api.Binding" );
        imports.add( "java.io.PrintStream" );
        imports.add( "org.osgi.framework.BundleContext" );
    }

    void resetAll() {
        resetCode();
        resetImports();
    }

    @Override
    public String getExecutableCode() {
        String finalLine = computeFinalLine();
        return Stream.concat( javaLines.stream(), tempJavaLines.stream() )
                .map( it -> it + ";\n" )
                .reduce( "", ( a, b ) -> a + b ) + finalLine;
    }

    @Override
    public String getMethodBody( String codeSnippet ) {
        return Stream.concat( javaLines.stream(), tempJavaLines.stream() )
                .map( it -> it + ";\n" )
                .reduce( "", ( a, b ) -> a + b ) + codeSnippet;
    }

    @Override
    public Collection<String> getImports() {
        return Stream.concat( imports.stream(), tempImports.stream() )
                .collect( Collectors.toSet() );
    }

    void commit() {
        while ( !tempJavaLines.isEmpty() &&
                tempJavaLines.getLast().startsWith( "return " ) ) {
            tempJavaLines.removeLast();
        }

        javaLines.addAll( tempJavaLines );
        tempJavaLines.clear();
        imports.addAll( tempImports );
        tempImports.clear();
    }

    void abort() {
        tempJavaLines.clear();
        tempImports.clear();
    }

    private String computeFinalLine() {
        String defaultFinalLine = "return null;\n";

        if ( tempJavaLines.isEmpty() ) {
            return defaultFinalLine;
        }

        String lastLine = tempJavaLines.get( tempJavaLines.size() - 1 );

        if ( lastLine.startsWith( "return " ) ) {
            return "";
        } else {
            return defaultFinalLine;
        }
    }
}
