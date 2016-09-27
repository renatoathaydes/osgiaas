package com.athaydes.osgiaas.javac.internal.compiler;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.javac.JavaSnippet;
import com.athaydes.osgiaas.javac.JavacService;
import com.athaydes.osgiaas.javac.internal.DefaultClassLoaderContext;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.IntSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@SuppressWarnings( "WeakerAccess" )
public class OsgiaasJavaCompilerServiceTest {

    public static String string;
    public static int integer;

    private final JavacService javacService = new OsgiaasJavaCompilerService();

    @Test
    public void canCompileSimpleJavaSnippets() throws Exception {
        String testClass = getClass().getName();
        Callable script = javacService.compileJavaSnippet(
                testClass + ".string = \"my first test\";return null;" )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        script.call();

        assertEquals( "my first test", string );

        script = javacService.compileJavaSnippet(
                testClass + ".integer = 23;return null;" )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        script.call();

        assertEquals( 23, integer );
    }

    @Test
    public void returnsValue() throws Exception {
        Callable script = javacService.compileJavaSnippet(
                "return 2 + 2;" )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

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

        Callable script = javacService.compileJavaSnippet( snippet )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

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
                JavaSnippet.Builder.withCode( snippet ).withImports( imports ) )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        Object result = script.call();

        assertEquals( Arrays.asList( 2, 4, 6 ), result );
    }

    @Test
    public void canDefineClass() throws Exception {
        String classDef = "public class Hello{ public static String get() {return \"hello\";}}";

        Class<?> cls = javacService.compileJavaClass( DefaultClassLoaderContext.INSTANCE,
                "Hello", classDef )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        Object instance = cls.newInstance();
        Method getMethod = cls.getMethod( "get" );
        Object result = getMethod.invoke( instance );

        assertEquals( "hello", result );

        classDef = "public class Hey{ public static String hey() {return Hello.get() + \"hey\";}}";

        cls = javacService.compileJavaClass( DefaultClassLoaderContext.INSTANCE,
                "Hey", classDef )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        instance = cls.newInstance();
        getMethod = cls.getMethod( "hey" );
        result = getMethod.invoke( instance );

        assertEquals( "hellohey", result );
    }

    @Test
    public void canCastClassToKnownInterfaceAndUsePackageName() throws Exception {
        String classDef = "package com.acme.util;" +
                "import java.util.function.IntSupplier;" +
                "public class ZeroSupplier implements IntSupplier {" +
                "public int getAsInt() { return 0; }" +
                "}";

        Class<?> cls = javacService.compileJavaClass( DefaultClassLoaderContext.INSTANCE,
                "com.acme.util.ZeroSupplier", classDef )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        IntSupplier instance = ( IntSupplier ) cls.newInstance();

        int zero = instance.getAsInt();

        assertEquals( 0, zero );
    }

    @Test
    public void canDefineNestedClass() throws Exception {
        String classDef = "public class A{ public static class B { public static String get() {return \"hello\";}}}";

        javacService.compileJavaClass( DefaultClassLoaderContext.INSTANCE,
                "A", classDef )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        ClassLoader augmentedClassLoader = javacService.getAugmentedClassLoaderContext(
                DefaultClassLoaderContext.INSTANCE ).getClassLoader();

        Class<?> abClass = augmentedClassLoader.loadClass( "A$B" );

        Object instance = abClass.newInstance();
        Method getMethod = abClass.getMethod( "get" );
        Object result = getMethod.invoke( instance );

        assertEquals( "hello", result );
    }

    @Test
    public void canDefineClassAfterCompilerError() throws Exception {
        String errorDef = "clazz Bad{ public String get() {return \"bad\";}}";
        String classDef = "class Good{ public String get() {return \"good\";}}";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream writer = new PrintStream( out );

        // compiler error
        try {
            javacService.compileJavaClass( DefaultClassLoaderContext.INSTANCE, "Bad", errorDef, writer );
            fail( "Should not have compiled class successfully" );
        } catch ( Throwable t ) {
            // ignore
        }

        // compile good class
        javacService.compileJavaClass( DefaultClassLoaderContext.INSTANCE, "Good", classDef, writer );

        // use good class
        Callable script = javacService.compileJavaSnippet(
                JavaSnippet.Builder.withCode( "return new Good().get();" ), DefaultClassLoaderContext.INSTANCE, writer )
                .orElseThrow( () -> new AssertionError( "Failed to compile class\n" + out ) );

        Object result = script.call();

        assertEquals( "good", result );
    }

    @Test
    public void canUseClassesFromProvidedClassLoaderInScript() throws Exception {
        // ensure the jar classLoader can see the Hello class
        ClassLoaderContext context = new HelloJarClassLoaderContext();
        Class helloClass = context.getClassLoader().loadClass( "Hello" );
        assertNotNull( helloClass );

        String snippet = "return Hello.hello();";

        Callable script = javacService.compileJavaSnippet(
                JavaSnippet.Builder.withCode( snippet ), context )
                .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

        Object result = script.call();

        assertEquals( "Hi", result );
    }

    private static class HelloJarClassLoaderContext implements ClassLoaderContext {

        private final ClassLoader classLoader;

        public HelloJarClassLoaderContext() {
            this.classLoader = new URLClassLoader( new URL[]{ getClass().getResource( "/hello-1.0.jar" ) } );
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public Collection<String> getClassesIn( String packageName ) {
            if ( packageName.isEmpty() ) {
                return Collections.singleton( "Hello.class" );
            }
            return Collections.emptyList();
        }
    }

}
