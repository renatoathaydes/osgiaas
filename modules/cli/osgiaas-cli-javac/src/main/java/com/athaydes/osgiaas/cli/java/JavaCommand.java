package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.api.stream.LineOutputStream;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.StreamingCommand;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import com.athaydes.osgiaas.cli.java.api.Binding;
import com.athaydes.osgiaas.javac.JavacService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCommand implements Command, StreamingCommand {

    private static final String JAVA_ID_REGEX = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";

    static final String RESET_CODE_ARG = "-r";
    static final String RESET_ALL_ARG = "-ra";
    static final String SHOW_ARG = "-s";

    static final String CLASS_ARG = "-c";

    private static final CommandHelper.CommandBreakupOptions JAVA_OPTIONS =
            CommandHelper.CommandBreakupOptions.create()
                    .includeQuotes( true );

    private Bundle bundle;
    private final JavacService javacService = JavacService.createDefault();
    private final JavaCode code = new JavaCode();
    private final Pattern lambdaIdentifierRegex =
            Pattern.compile(
                    "\\s*(\\()?\\s*(?<id>" + JAVA_ID_REGEX + ")\\s*(\\))?\\s*->(?<body>.+)",
                    Pattern.DOTALL );

    private ClassLoaderCapabilities classLoaderContext;

    private final ArgsSpec javaArgs = ArgsSpec.builder()
            .accepts( RESET_CODE_ARG )
            .accepts( RESET_ALL_ARG )
            .accepts( SHOW_ARG )
            .accepts( CLASS_ARG )
            .build();

    private static final Callable<?> ERROR = () -> null;

    public void setClassLoaderContext( ClassLoaderCapabilities classLoaderContext ) {
        this.classLoaderContext = classLoaderContext;
    }

    public void activate( BundleContext context ) {
        this.bundle = context.getBundle();
    }

    public void deactivate( BundleContext context ) {
        this.bundle = null;
    }

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
                "< Person(Mary, 24)\n\n" +
                "When run through pipes, the Java snippet should be a Function<String, ?> that takes " +
                "each input line as an argument, returning something to be printed (or null).\n'n" +
                "Example:\n" +
                "> some_command | java line -> line.contains(\"text\") ? line : null";
    }

    JavaAutocompleteContext getAutocompleContext() {
        return code;
    }

    @Override
    public OutputStream pipe( String command, PrintStream out, PrintStream err ) {
        CommandInvocation invocation = javaArgs.parse( command,
                JAVA_OPTIONS.separatorCode( ' ' ) );

        Matcher identifierMatcher = lambdaIdentifierRegex.matcher( invocation.getUnprocessedInput() );

        if ( identifierMatcher.matches() ) {
            String lambdaArgIdentifier = identifierMatcher.group( "id" );
            String lambdaBody = identifierMatcher.group( "body" ).trim();

            if ( lambdaBody.endsWith( ";" ) ) {
                lambdaBody = lambdaBody.substring( 0, lambdaBody.length() - 1 );
            }

            String lineConsumerCode = "return (java.util.function.Function<String, ?>)" +
                    "((" + lambdaArgIdentifier.trim() + ") -> " + lambdaBody + ");";

            Optional<Callable<?>> callable = javacService
                    .compileJavaSnippet( lineConsumerCode, classLoaderContext, err );
            if ( callable.isPresent() ) {
                try {
                    Function<String, ?> callback = ( Function<String, ?> ) callable.get().call();
                    return new LineOutputStream( line -> {
                        @Nullable Object output = callback.apply( line );
                        if ( output != null ) {
                            out.println( output );
                        }
                    }, out );
                } catch ( Exception e ) {
                    err.println( e );
                }
            }
        }

        throw new RuntimeException( "When used in a pipeline, the Java snippet must be in the form of a " +
                "lambda of type Function<String, ?> that takes one text line of the input at a time.\n" +
                "Example: ... | java line -> line.contains(\"text\") ? line : null" );
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        Objects.requireNonNull( classLoaderContext, "Did not set ClassLoaderCapabilities" );

        CommandInvocation invocation = javaArgs.parse( line,
                JAVA_OPTIONS.separatorCode( ' ' ) );

        if ( invocation.hasArg( RESET_CODE_ARG ) ) {
            code.clearCode();
        }

        if ( invocation.hasArg( RESET_ALL_ARG ) ) {
            code.clearAll();
        }

        String codeToRun = invocation.getUnprocessedInput();
        @Nullable String className;

        if ( invocation.hasArg( CLASS_ARG ) && ( className = extractClassName( codeToRun, err ) ) != null ) {
            Optional<Class<Object>> javaClass = javacService.compileJavaClass(
                    classLoaderContext, className, codeToRun, err );
            javaClass.ifPresent( out::println );
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
            // ignore error, let the compiler provide an error message
            return "Err";
        }

        return null;
    }

    private void runJava( String input, PrintStream out, PrintStream err ) {
        breakupJavaLines( input );
        Binding.out = out;
        Binding.err = err;
        Binding.ctx = bundle.getBundleContext();

        try {
            Callable<?> callable = javacService.compileJavaSnippet(
                    code, classLoaderContext, err
            ).orElse( ERROR );

            if ( callable != ERROR ) {
                code.commit();
                Object result = callable.call();

                if ( result != null ) {
                    out.println( result );
                }
            } else {
                code.abort();
            }
        } catch ( Throwable e ) {
            code.abort();
            e.printStackTrace( err );
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
