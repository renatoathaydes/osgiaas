# OSGiaaS Jar-Wrap Module

> Status: beta

This module is a simple Java API for wrapping regular jars into OSGi bundles.

It uses [Bnd](http://www.aqute.biz/Bnd/Bnd) to perform the wrapping.

This module can be run as a regular Jar or a OSGi bundle.

## Usage

The `JarWrapper` class is responsible for wrapping jars. Each instance of this class is expected to "wrap" a single jar.

The following illustrates the simplest possible usage:

```java
import com.athaydes.osgiaas.wrap.JarWrapper;
import java.io.File;

class JarWrapperExample {
    public static void main(String[] args){
      File bundle = new JarWrapper(new File("simple.jar")).wrap("1.0");
      System.out.println("Wrapped jar to " + bundle);
    }
}
```

The desired version of the bundle must be provided to the `wrap` method.

The `JarWrapper` class contains several methods that allow configuring how the wrapping should be performed.
These setters are supposed to be used as in a builder.

Example configuring everything possible:

```java
import com.athaydes.osgiaas.wrap.JarWrapper;
import java.io.File;

class JarWrapperExample {
    public static void main(String[] args){
      File bundle = new JarWrapper(new File("simple.jar"))
        .setDestination(new File("bundle.jar"))
        .setArtifactName("my-bundle")
        .setImportInstructions("*") // import everything the jar uses
        .setExportInstructions("com.acme.util, com.acme.shared") // export only these packages
        .wrap("1.0");
      System.out.println("Wrapped jar to " + bundle);
    }
}
```

## Important Note

If the provided jar is already a bundle, the file will be returned as-is. Therefore, you might want to check if the
file is a bundle before attempting to wrap it, which can be done by calling the 
`JarWrapper.isBundle(file)` method.
