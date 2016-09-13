# OSGiaaS JavaScript Module

A module that implements a JavaScript REPL based on [Nashorn](http://openjdk.java.net/projects/nashorn/).

## Usage:

Simply type `js` following by any JavaScript code.

For example:

```
>> js function double(x) { return x * 2 };
function double(x) { return x * 2 }
>> js double(4)
8
```

## System requirements:

The OSGi framework may not export the `sun.reflect,jdk.nashorn.api.scripting` package, which is required by Nashorn.

If you get errors related to this when starting your OSGi container, add this package to the
`org.osgi.framework.system.packages.extra` OSGi property.

With the `osgi-run` Gradle plugin, you can achieve this by adding this line to your build file:

```groovy
runOsgi {
    // ... other settings

    config += [ 'org.osgi.framework.system.packages.extra': 'sun.reflect,jdk.nashorn.api.scripting' ]
}
```
