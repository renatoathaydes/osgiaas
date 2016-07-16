package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandInvocation;
import com.athaydes.osgiaas.api.cli.args.ArgsSpec;
import com.athaydes.osgiaas.javac.JavacService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class JavaCommand implements Command {

    private static final String RESET_CODE_ARG = "-r";
    private static final String RESET_ALL_ARG = "-ra";
    private static final String SHOW_ARG = "-s";
    private static final String CLASS_ARG = "-c";

    private static final CommandHelper.CommandBreakupOptions JAVA_OPTIONS =
            CommandHelper.CommandBreakupOptions.create()
                    .includeQuotes( true );

    private final JavacService javacService = JavacService.createDefault();
    private final JavaCode code = new JavaCode();
    private final ArgsSpec javaArgs = ArgsSpec.builder()
            .accepts( RESET_CODE_ARG )
            .accepts( RESET_ALL_ARG )
            .accepts( SHOW_ARG )
            .accepts( CLASS_ARG )
            .build();

    @Override
    public String getName() {
        return "java";
    }

    @Override
    public String getUsage() {
        return "java [-r | -s | <java snippet>] | " +
                "[-c <class definition>]";
    }

    @Override
    public String getShortDescription() {
        return "Run Java code statements.\n" +
                "\nAll statements entered earlier are executed each time a new statement is entered.\n" +
                "The previous statements can be forgotten with the -r (reset) option." +
                "\n\n" +
                "The java command accepts the following flags:\n" +
                "  \n" +
                "  * " + RESET_CODE_ARG + ": reset the current code statement buffer.\n" +
                "  * " + RESET_ALL_ARG + ": reset the current code statement buffer and imports.\n" +
                "  * " + SHOW_ARG + ": show the current statement buffer.\n" +
                "  * " + CLASS_ARG + ": define a class.\n" +
                "\n" +
                "Simple example:\n\n" +
                "> java return 2 + 2\n" +
                "< 4\n\n" +
                "Multi-line example to define a separate class:\n\n" +
                "> :{\n" +
                "java -c class Person {\n" +
                "  String name;\n" +
                "  int age;\n" +
                "  Person(String name, int age) {\n" +
                "    this.name = name;\n" +
                "    this.age = age;\n" +
                "  }\n" +
                "  public String toString() { return \"Person(\" + name + \",\" + age + \")\"; }" +
                "}\n" +
                ":}\n" +
                "<\n" +
                "> java return new Person(\"Mary\", 24);\n" +
                "< Person(Mary, 24)\n";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        CommandInvocation invocation = javaArgs.parse( line,
                JAVA_OPTIONS.separatorCode( ' ' ) );

        if ( invocation.hasArg( RESET_CODE_ARG ) ) {
            code.clearCode();
        }

        if ( invocation.hasArg( RESET_ALL_ARG ) ) {
            code.clearAll();
        }

        String codeToRun = invocation.getUnprocessedInput();

        if ( invocation.hasArg( CLASS_ARG ) ) {
            @Nullable String className = extractClassName( codeToRun, err );
            if ( className != null ) {
                out.println( javacService.compileJavaClass(
                        getClass().getClassLoader(), className, codeToRun ) );
            }
        } else {
            boolean show = invocation.hasArg( SHOW_ARG );

            if ( show ) {
                out.println( javacService.getJavaSnippetClass( code ) );
            }

            // run the current code if some input was given, or in any case when no show option
            if ( !codeToRun.isEmpty() || !show ) {
                runJava( codeToRun, out, err );
            }
        }

    }

    @Nullable
    private static String extractClassName( String code, PrintStream err ) {
        InputStream inputStream = new ByteArrayInputStream( code.getBytes( StandardCharsets.UTF_8 ) );
        try {
            CompilationUnit compilationUnit = JavaParser.parse( inputStream );
            List<TypeDeclaration> types = compilationUnit.getTypes();
            if ( types.size() == 1 ) {
                String simpleType = types.get( 0 ).getName();
                return Optional.ofNullable( compilationUnit.getPackage() )
                        .map( PackageDeclaration::getPackageName )
                        .map( it -> it + "." + simpleType )
                        .orElse( simpleType );
            } else if ( types.size() == 0 ) {
                err.println( "No class definition found" );
            } else {
                err.println( "Too many class definitions found. Only one class can be defined at a time." );
            }
        } catch ( ParseException e ) {
            e.printStackTrace( err );
        }

        return null;
    }

    private void runJava( String input, PrintStream out, PrintStream err ) {
        breakupJavaLines( input );

        try {
            Object result = javacService.compileJavaSnippet(
                    code, getClass().getClassLoader(), new PrintWriter( err )
            ).call();

            code.commit();

            if ( result != null ) {
                out.println( result );
            }
        } catch ( Throwable e ) {
            code.abort();
            err.println( e );
        }
    }

    private void breakupJavaLines( String input ) {
        CommandHelper.breakupArguments( input, ( javaLine ) -> {
            javaLine = javaLine.trim();
            if ( !javaLine.isEmpty() ) {
                code.addLine( javaLine );
            }
            return true;
        }, JAVA_OPTIONS.separatorCode( ';' ) );
    }


}
