package com.athaydes.osgiaas.javac;

import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings( "WeakerAccess" )
public class JavacServiceTest {

    public static String string;
    public static int integer;

    private final JavacService javacService = new JavacService();

    @Test
    public void canCompileSimpleJavaSnippets() throws Exception {
        String testClass = getClass().getName();
        Callable script = javacService.compileJavaSnippet(
                testClass + ".string = \"my first test\";return null;" );

        script.call();

        assertEquals( "my first test", string );

        script = javacService.compileJavaSnippet(
                testClass + ".integer = 23;return null;" );

        script.call();

        assertEquals( 23, integer );
    }

    @Test
    public void returnsValue() throws Exception {
        Callable script = javacService.compileJavaSnippet(
                "return 2 + 2;" );

        Object result = script.call();

        assertEquals( 4, result );
    }

    @Test
    public void canCompileMultilineJavaSnippets() throws Exception {
        String testClass = getClass().getName();

        String snippet = "int i = 10;\n" +
                "int j = 20;\n" +
                "int k = i * j;\n" +
                testClass + ".string = \"i + j = \" + k;\n" +
                "return k;";

        Callable script = javacService.compileJavaSnippet( snippet );

        Object result = script.call();

        assertEquals( "i + j = " + ( 10 * 20 ), string );
        assertEquals( 10 * 20, result );
    }

    @Test
    public void canUseImportsInJavaSnippets() throws Exception {
        List<String> imports = Arrays.asList( "java.util.Arrays", "java.util.List" );
        String snippet = "List<Integer> ints = Arrays.asList(2, 4, 6);\n" +
                "return ints;";

        Callable script = javacService.compileJavaSnippet(
                JavaSnippet.Builder.withCode( snippet ).withImports( imports ) );

        Object result = script.call();

        assertEquals( Arrays.asList( 2, 4, 6 ), result );
    }

    @Test
    public void canDefineClass() throws Exception {
        String classDef = "class Hello{ public static String get() {return \"hello\";}}";

        Callable script = javacService.compileJavaSnippet(
                JavaSnippet.Builder.withCode( "return Hello.get();" )
                        .withClassDefinitions( Collections.singleton( classDef ) ) );

        Object result = script.call();

        assertEquals( "hello", result );
    }

    @Test
    @Ignore( "Waiting for fix of bug: https://github.com/OpenHFT/Java-Runtime-Compiler/issues/12" )
    public void canDefineClassAfterCompilerError() throws Exception {
        String errorDef = "clazz Bad{ public String get() {return \"bad\";}}";
        String classDef = "class Good{ public String get() {return \"good\";}}";

        // use our own classloader to be more realistic
        ClassLoader classLoader = new URLClassLoader( new URL[]{ } );

        // compiler error
        try {
            javacService.compileJavaSnippet(
                    JavaSnippet.Builder.withCode( "return null;" )
                            .withClassDefinitions( Collections.singleton( errorDef ) ),
                    classLoader );
            fail( "Should not have compiled class successfully" );
        } catch ( Throwable t ) {
            // ignore
        }

        // compile good class
        javacService.compileJavaSnippet(
                JavaSnippet.Builder.withCode( "return null;" )
                        .withClassDefinitions( Collections.singleton( classDef ) ),
                classLoader );

        // use good class
        Callable script = javacService.compileJavaSnippet(
                JavaSnippet.Builder.withCode( "return new Good().get();" ) );

        Object result = script.call();

        assertEquals( "good", result );
    }


}
