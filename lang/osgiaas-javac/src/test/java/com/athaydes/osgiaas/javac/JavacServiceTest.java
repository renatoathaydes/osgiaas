package com.athaydes.osgiaas.javac;

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;

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


}
