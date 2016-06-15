package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.javac.JavacService;
import org.apache.felix.shell.Command;

import java.io.PrintStream;

public class JavaCommand implements Command {

    private final JavacService javacService = new JavacService();

    @Override
    public String getName() {
        return "java";
    }

    @Override
    public String getUsage() {
        return "java <java snippet>";
    }

    @Override
    public String getShortDescription() {
        return "Run a Java Snippet";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        String code = line.trim().substring( getName().length() );
        try {
            Object result = javacService
                    .compileJavaSnippet( code, getClass().getClassLoader() )
                    .call();

            if ( result != null ) {
                out.println( result );
            }
        } catch ( Exception e ) {
            err.println( e.getCause() );
        }
    }
}
