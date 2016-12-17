# OSGiaaS CLI Ivy Module

The OSGIaaS CLI Ivy Module uses [Apache Ivy](http://ant.apache.org/ivy/) to implement a command
which can download artifacts from repositories such as
[JCenter](https://jcenter.bintray.com/) (enabled by default) and [Maven Central](https://repo1.maven.org/maven2).

Basic usage:

```
ivy [-i] [-a] [-n] [-r… <repo-url>] group:module[:version]
```

## Command Options

The ivy command supports the following options:

  * `[-i]`, `[--intransitive]`
    do not retrieve transitive dependencies
  * `[-a]`, `[--download-all]`
    download also javadocs and sources jars if available
  * `[-n]`, `[--no-maven-local]`
    do not use the Maven local repository
  * `[-r…]`, `[--repository…] <repo-url>`
    specify repositories to use to search for artifacts (uses JCenter by default)

## Examples

>> ivy -i io.javaslang:javaslang:2.1.0-alpha
< file:///home/username/.ivy2/cache/io.javaslang/javaslang/jars/javaslang-2.1.0-alpha.jar

The artifact's version can be omitted, in which case the latest version is downloaded.
The output of the ivy command is a file URL that can be recognized by the 'install' and 'start' commands.
Example to download and immediately start a library:

>> ivy io.javaslang:javaslang:2.1.0-alpha | start
