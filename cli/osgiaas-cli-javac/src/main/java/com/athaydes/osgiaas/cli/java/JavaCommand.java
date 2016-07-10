package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandInvocation;
import com.athaydes.osgiaas.api.cli.args.ArgsSpec;
import com.athaydes.osgiaas.javac.JavacService;
import org.apache.felix.shell.Command;

import java.io.PrintStream;

public class JavaCommand implements Command {

    private static final String RESET_ARG = "-r";
    private static final String SHOW_ARG = "-s";

    private static final CommandHelper.CommandBreakupOptions JAVA_OPTIONS =
            CommandHelper.CommandBreakupOptions.create()
                    .includeQuotes( true )
                    .includeSeparators( false )
                    .separatorCode( ';' );

    private final JavacService javacService = new JavacService();
    private final JavaCode code = new JavaCode();
    private final ArgsSpec javaArgs = ArgsSpec.builder()
            .accepts( RESET_ARG )
            .accepts( SHOW_ARG )
            .build();

    @Override
    public String getName() {
        return "java";
    }

    @Override
    public String getUsage() {
        return "java -r | -s | <java snippet>";
    }

    @Override
    public String getShortDescription() {
        return "Run Java code statements.\n" +
                "\nAll statements entered earlier are executed each time a new statement is entered.\n" +
                "The previous statements can be forgotten with the -r (reset) option." +
                "\n\n" +
                "The java command accepts the following flags:\n" +
                "  \n" +
                "  * -r: reset the current statement buffer.\n" +
                "  * -s: show the current statement buffer.\n" +
                "\n" +
                "Simple example:\n\n" +
                "> java return 2 + 2\n" +
                "< 4\n\n" +
                "Multi-line example:\n\n" +
                "> :{\n" +
                "java class Person {\n" +
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
        // FIXME this is breaking multi-line input, the first line's separator is removed
        CommandInvocation invocation = javaArgs.parse( line, JAVA_OPTIONS );

        if ( invocation.hasArg( RESET_ARG ) ) {
            code.clear();
        }

        if ( invocation.hasArg( SHOW_ARG ) ) {
            out.println( javacService.getJavaSnippetClass(
                    code.getExecutableCode(), code.getImports() ) );
        }

        if ( !invocation.getUnprocessedInput().isEmpty() ) {
            runJava( invocation.getUnprocessedInput(), out, err );
        }
    }

    private void runJava( String input, PrintStream out, PrintStream err ) {
        breakupJavaLines( input );

        try {
            Object result = javacService.compileJavaSnippet(
                    code.getExecutableCode(), code.getImports(), getClass().getClassLoader()
            ).call();

            code.commit();

            if ( result != null ) {
                out.println( result );
            }
        } catch ( Throwable e ) {
            code.abort();
            err.println( e.getCause() );
        }
    }

    private void breakupJavaLines( String input ) {
        CommandHelper.breakupArguments( input, ( javaLine ) -> {
            javaLine = javaLine.trim();
            if ( !javaLine.isEmpty() ) {
                code.addLine( javaLine );
            }
            return true;
        }, JAVA_OPTIONS );
    }


}
