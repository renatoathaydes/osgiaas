package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCommand implements Command, StreamingCommand {

    private static final String JAVA_ID_REGEX = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";

    static final String RESET_CODE_ARG = "-r";
    static final String RESET_CODE_LONG_ARG = "--reset";
    static final String RESET_ALL_ARG = "-t";
    static final String RESET_ALL_LONG_ARG = "--reset-all";
    static final String SHOW_ARG = "-s";
    static final String SHOW_LONG_ARG = "--show";
    static final String CLASS_ARG = "-c";
    static final String CLASS_LONG_ARG = "--class-define";

    private static final CommandHelper.CommandBreakupOptions JAVA_OPTIONS =
            CommandHelper.CommandBreakupOptions.create()
                    .includeQuotes( true );

    private Bundle bundle;
    private final JavacService javacService = JavacService.createDefault();
    private final JavaCode code;
    private final Pattern lambdaIdentifierRegex =
            Pattern.compile(
                    "\\s*(\\()?\\s*(?<id>" + JAVA_ID_REGEX + ")\\s*(\\))?\\s*->(?<body>.+)",
                    Pattern.DOTALL );

    private ClassLoaderCapabilities classLoaderContext;

    private final ArgsSpec javaArgs = ArgsSpec.builder()
            .accepts( RESET_CODE_ARG, RESET_CODE_LONG_ARG )
            .withDescription( "reset the current code statement buffer" ).end()
            .accepts( RESET_ALL_ARG, RESET_ALL_LONG_ARG )
            .withDescription( "reset the current code statement buffer and imports" ).end()
            .accepts( SHOW_ARG, SHOW_LONG_ARG )
            .withDescription( "show the current statement buffer" ).end()
            .accepts( CLASS_ARG, CLASS_LONG_ARG )
            .withDescription( "define a Java class" ).end()
            .build();

    @SuppressWarnings( "ConstantConditions" )
    private static final Callable<?> ERROR = () -> null;

    public JavaCommand() {
        this.code = new JavaCode( () -> Optional.ofNullable( getClassLoaderContext() ) );
    }

    public ClassLoaderContext getClassLoaderContext() {
        return javacService.getAugmentedClassLoaderContext( classLoaderContext );
    }

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
        return "java " + javaArgs.getUsage() + " <Java code>";
    }

    @Override
    public String getShortDescription() {
        return "Run Java code statements.\n" +
                "\nAll statements entered previously, except return statements, are executed each time " +
                "a new statement is entered.\n" +
                "Previous statements can be forgotten with the -r (reset) option.\n" +
                "Permanent variables can be created by adding them to the 'binding' Map, whose contents\n" +
                "get expanded into local variables on execution." +
                "\n\n" +
                "The java command accepts the following options:\n\n" +
                javaArgs.getDocumentation( "  " ) + "\n\n" +
                "Simple example:\n\n" +
                ">> java return 2 + 2\n" +
                "< 4\n\n" +
                "Binding example:\n\n" +
                ">> java binding.put(\"var\", 10);\n" +
                ">> java -r return var + 10; // var is still present even after using the -r option\n" +
                "< 20\n\n" +
                "Multi-line example to define a separate class (notice that when using the -c option, " +
                "a Java class or interface must be defined):\n\n" +
                ">> :{\n" +
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
                "< class Person\n" +
                ">> java return new Person(\"Mary\", 24);\n" +
                "< Person(Mary, 24)\n\n" +
                "When run through pipes, the Java snippet should return a Function<String, ?> that takes \n" +
                "each input line as an argument, returning something to be printed (or null).\n\n" +
                "Example:\n\n" +
                ">> some_command | java line -> line.contains(\"text\") ? line : null\n";
    }

    JavaCode getAutocompleContext() {
        return code;
    }

    @Override
    public Consumer<String> pipe( String command, PrintStream out, PrintStream err ) {
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
                    "((" + lambdaArgIdentifier.trim() + ") -> " + lambdaBody + ")";

            JavaCode functionCode = new JavaCode( code );
            functionCode.addLine( lineConsumerCode );

            Optional<Callable<?>> callable = javacService
                    .compileJavaSnippet( functionCode, classLoaderContext, err );

            if ( callable.isPresent() ) {
                try {
                    //noinspection unchecked
                    Function<String, ?> callback = ( Function<String, ?> ) callable.get().call();
                    return line -> {
                        @Nullable Object output = callback.apply( line );
                        if ( output != null ) {
                            out.println( output );
                        }
                    };
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

        if ( invocation.hasOption( RESET_CODE_ARG ) ) {
            code.resetCode();
        }

        if ( invocation.hasOption( RESET_ALL_ARG ) ) {
            code.resetAll();
        }

        String codeToRun = invocation.getUnprocessedInput();
        @Nullable String className;

        if ( invocation.hasOption( CLASS_ARG ) && ( className = extractClassName( codeToRun, err ) ) != null ) {
            Optional<Class<Object>> javaClass = javacService.compileJavaClass(
                    classLoaderContext, className, codeToRun, err );
            javaClass.ifPresent( out::println );
        } else {
            breakupJavaLines( codeToRun );

            boolean show = invocation.hasOption( SHOW_ARG );

            if ( show ) {
                out.println( javacService.getJavaSnippetClass( code ) );
                if ( !codeToRun.isEmpty() ) {
                    // add new lines between the code and its result
                    out.println();
                }
            }

            // run the current code if some input was given, or in any case when no show option
            if ( !codeToRun.isEmpty() || !show ) {
                runJava( out, err );
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

    private void runJava( PrintStream out, PrintStream err ) {
        Binding.out = out;
        Binding.err = err;
        Binding.ctx = bundle.getBundleContext();

        try {
            Callable<?> callable = javacService.compileJavaSnippet(
                    code, classLoaderContext, err
            ).orElse( ERROR );

            if ( callable != ERROR ) {
                Object result = callable.call();

                if ( result != null ) {
                    out.println( result );
                }

                code.commit();
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
            if ( !javaLine.isEmpty() && !javaLine.startsWith( "//" ) ) {
                code.addLine( javaLine );
            }
            return true;
        }, JAVA_OPTIONS.separatorCode( ';' ) );
    }
}
