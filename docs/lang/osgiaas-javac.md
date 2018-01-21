# OSGiaaS Javac Module

> Status: beta

This module implements a in-memory Java compiler service based on the JVM's own
[JavaCompiler](https://docs.oracle.com/javase/7/docs/api/javax/tools/JavaCompiler.html).

It never reads or writes to disk, so it is blazing fast.

The [osgiaas-cli-javac](../cli/osgiaas-cli-javac.md) module, which lets you run Java code as in a REPL,
is based on this compiler.

## System Requirements

Not all JREs provide the `JavaCompiler` class and the other Java tools required for this module to work.

If you cannot run your system using a full JDK, add the `tools.jar` file (part of the JDK, but not usually the JRE)
to your JRE's `ext` folder and the compiler should work.

This module uses `slf4j` to log, so the runtime must provide a `slf4j` implementation such as `log4j` or, in OSGi
environments, the [`slf4j-to-osgi-log`](../infra/slf4j-to-osgi-log.md) module.

## Basic usage

Using the Javac service is as easy as making one method call:

```java
import com.athaydes.osgiaas.javac.JavacService;

JavacService javac = JavacService.createDefault();

Optional<Callable<?>> result = javac.compileJavaSnippet( "return 2 + 2;" );
```

Running the result `Callable<?>` above should return `4`.

To compile a full class is almost as easy:

```java
String classDef = "public class Hello{ public static String get() {return \"hello\";}}";

Class<?> cls = javacService.compileJavaClass( DefaultClassLoaderContext.INSTANCE,
        "Hello", classDef )
        .orElseThrow( () -> new AssertionError( "Failed to compile class" ) );

Object instance = cls.newInstance();
Method getMethod = cls.getMethod( "get" );
Object result = getMethod.invoke( instance );

assertEquals( "hello", result );
```

If the class you're compiling implements a known interface, such as `IntSupplier`,
you can cast the Object returned by calling
`newInstance()` on the compiled class, and then use it type-safely as in the following example:

> This example shows that you can even declare a package name for your class!

```java
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
```

# Advanced usage

If you need more control, you can use the `com.athaydes.osgiaas.javac.internal.compiler.OsgiaasJavaCompiler`
class directly.

The following code shows how you can pass an option, `-Werror` (treat compiler warnings as errors)
directly to `javac`, compile a class that implements
`Runnable`, then run an instance of the compiled class:

```java
OsgiaasJavaCompiler compiler = new OsgiaasJavaCompiler( DefaultClassLoaderContext.INSTANCE,
   Arrays.asList( "-Werror" ) );

Optional<Class<Object>> runner = compiler.compile( "Runner", "public class Runner implements Runnable {" +
        "  public void run() {" +
        "    System.out.println(\"hello world\");" +
        "  }" +
        "}", System.out );

runner.orElseThrow( () -> new RuntimeException( "Error compiling" ) )
        .asSubclass( Runnable.class )
        .newInstance()
        .run();
```

If you want control over how classes are loaded into the JVM, you can also provide a custom implementation of
`ClassLoaderContext`, which wraps a `ClassLoader` and provides information to the compiler about classes the
`ClassLoader` can load.

See [OsgiaasClassLoader](https://github.com/renatoathaydes/osgiaas/blob/master/modules/lang/osgiaas-javac/src/main/java/com/athaydes/osgiaas/javac/internal/compiler/OsgiaasClassLoader.java)
for an example context implementation.
