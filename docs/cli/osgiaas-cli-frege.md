# OSGiaaS CLI Frege Module

A module that implements a [Frege](https://github.com/Frege/frege) REPL.

> Frege is a Haskell for the JVM

## Usage

Simply type `frege` followed by any Frege code.

For example:

```
>> frege f x = x * 2
>> frege f 4
8
```

> Note: the Frege interpreter is very slow in the beginning, but as you use it, it gets much faster

## System requirements

* The Frege runtime does not work nicely within an OSGi environment. For this reason, the Frege runtime should be
installed as a system library, rather than a simple OSGi bundle.

* It is also necessary to provide an implementation of the `slf4j-api` at runtime. The
  [slf4j-to-osgi-log](../infra/slf4j-to-osgi-log.md) module may be used for that.

With the `osgi-run` Gradle plugin, you can meet both requirements by adding the following dependencies in
your Gradle file:

```groovy
dependencies {
    // ... core dependencies
    osgiRuntime 'com.athaydes.osgiaas:osgiaas-cli-frege:1.0-SNAPSHOT'
    systemLib 'org.frege-lang:frege-interpreter-core:1.2'

    osgiRuntime 'com.athaydes.osgiaas:slf4j-to-osgi-log:1.7.0'
}
```

See [osgiaas-cli-frege.gradle](../../samples/osgiaas-cli-frege.gradle) for a minimal build file.

## Multi-line and *use* example

The OSGiaaS CLI supports multi-line commands, just start by typing `:{` and finish by typing `:}` in the CLI.

It also lets you *use* commands, which means you don't need to type the
command every time... using these two features, we can get a quite nice REPL:

```
>> use frege
Using 'frege'. Type _use to stop using it.
[using frege]
>> :{
module Hello where
greeting friend = "Hello, " ++ friend ++ "!"
:}
[using frege]
>> import Hello
[using frege]
>> greeting "John"
Hello, John!
```
