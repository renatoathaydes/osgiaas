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
                testClass + ".string = \"my first test\";" );

        script.call();

        assertEquals( "my first test", string );

        script = javacService.compileJavaSnippet(
                testClass + ".integer = 23;" );

        script.call();

        assertEquals( 23, integer );
    }

    @Test
    public void returnsValue() throws Exception {
        Callable script = javacService.compileJavaSnippet(
                "2 + 2" );

        Object result = script.call();

        assertEquals( 4, result );
    }

    @Test
    public void doesNotRequireReturnValue() throws Exception {
        Callable script = javacService.compileJavaSnippet(
                "System.out.println(123);" );

        script.call();
    }

}
