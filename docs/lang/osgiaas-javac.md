# OSGiaaS Javac Module

This module implements a in-memory Java compiler service based on the JVM's own
[JavaCompiler](https://docs.oracle.com/javase/7/docs/api/javax/tools/JavaCompiler.html).

It never reads or writes to disk, so it is blazing fast.

The [osgiaas-cli-javac](../cli/osgiaas-cli-javac.md) module, which lets you run Java code as in a REPL,
is based on this compiler.

## System Requirements

Not all JREs provide the JavaCompiler class and the other Java tools required for this module to work.

If you cannot run your system using a full JDK, add the `tools.jar` file (part of the JDK, but not usually the JRE)
to your JRE's `ext` folder and the compiler should work.

## Basic usage

Using the Javac service is as easy as making one method call:

```java
import com.athaydes.osgiaas.javac.JavacService;

JavacService javac = JavacService.createDefault();

Optional<Callable<?>> result = javac.compileJavaSnippet( "return 2 + 2;" );
```

Running the result `Callable<?>` above should return `4`.

# Advanced usage

TBD