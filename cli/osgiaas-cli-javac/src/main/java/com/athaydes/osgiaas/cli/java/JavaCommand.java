package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandInvocation;
import com.athaydes.osgiaas.api.cli.args.ArgsSpec;
import com.athaydes.osgiaas.javac.JavacService;
import org.apache.felix.shell.Command;

import java.io.PrintStream;

public class JavaCommand implements Command {

    private static final String RESET_CODE_ARG = "-r";
    private static final String RESET_ALL_ARG = "-ra";
    private static final String SHOW_ARG = "-s";
    private static final String CLASS_ARG = "-c";

    private static final CommandHelper.CommandBreakupOptions JAVA_OPTIONS =
            CommandHelper.CommandBreakupOptions.create()
                    .includeQuotes( true );

    private final JavacService javacService = new JavacService();
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
            String classDef = codeToRun;
            code.setClassDef( classDef );

            // let neutral code run to validate the class definition
            codeToRun = "return null;";
        }

        if ( invocation.hasArg( SHOW_ARG ) ) {
            out.println( javacService.getJavaSnippetClass( code ) );
        }

        if ( !codeToRun.isEmpty() ) {
            runJava( codeToRun, out, err );
        }
    }

    private void runJava( String input, PrintStream out, PrintStream err ) {
        breakupJavaLines( input );

        try {
            Object result = javacService.compileJavaSnippet(
                    code, getClass().getClassLoader()
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
