# OSGiaaS CLI Grab Module

The OSGIaaS CLI Grab Module uses the [osgiaas-grab](../infra/osgiaas-grab.md) module to implement a command
which can download (or grab) Maven dependencies from any Maven repository such as
[JCenter](https://jcenter.bintray.com/) (enabled by default) and [Maven Central](https://repo1.maven.org/maven2).

Basic usage:

```
grab <options> <artifact-coordinates> | <sub-command> <arg>
```

Artifact coordinates, composed of groupId, artifactId, and version (optionally, with a classifier),
should be joined with ':'.

Example:

```
>> grab com.google.guava:guava:19.0
file:///user/.groovy/grapes/com.google.guava/guava/jars/guava-19.0.jar
```

The grab command prints the full path to all files downloaded after it grabs a dependency.
This is handy as the output of `grab` can be given directly to the `install` or `start` command to have it installed
onto the OSGi environment, or just started immediately.

> All mandatory dependencies of an artifact are also grabbed within a single execution

The following command will not only download Guava and all its dependencies, but will also install and start
the bundles (even wrapping the plain jars into OSGi bundles if necessary):

```
>> grab com.google.guava:guava:19.0 | start
```

## Command Options

The following options are supported:

  * -v : verbose mode. Prints information about downloads.

## Sub-commands

Grab also supports the following sub-commands:

  * --add-repo [<repo-id>] <repo> : adds a repository to grab artifacts from.
  * --rm-repo <repo-id> : removes a repository.
  * --list-repos <repo-id> : lists existing repositories.

If <repo-id> is not given, the repo address is also used as its ID.

Example:

```
>> grab --add-repo spring http://repo.spring.io/release
List of repositories:
  * spring: spring
  * default: https://jcenter.bintray.com/
```


