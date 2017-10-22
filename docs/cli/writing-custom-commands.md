# Writing Custom Commands

## Basics

The OSGiaaS-CLI uses the [Apache Felix Shell](http://felix.apache.org/documentation/subprojects/apache-felix-shell.html)
system to discover and run commands.

So, all you need to create a basic command is the following:

* write an implementation of the `org.apache.felix.shell.Command` interface.
* publish this implementation as a OSGi service providing the `org.apache.felix.shell.Command` interface.

> See the Tutorial section below for instructions on how you can easily do this!

## Supporting shell pipelines

To get your command implementation to support efficient pipes, you also need to:

* make your command also implement the `com.athaydes.osgiaas.cli.StreamingCommand` interface.

> If your Command does not provide the `StreamingCommand` interface, it will still work in pipes but it will probably
  be less efficient than it should be as it will have to wait for the previous command in the pipeline to finish execution
  completely, so that it can run as if invoked with the full output of the previous command as its input.
  By implementing `StreamingCommand`, your command can accept a line of input at a time, unclogging the pipeline.

## Supporting auto-completion

For your command to get auto-completion support, you need to:

* write an implementation of the `com.athaydes.osgiaas.cli.CommandCompleter` interface.
* publish this implementation as a OSGi service providing the `com.athaydes.osgiaas.cli.CommandCompleter` interface.

Most of the time, you don't need to write a `CommandCompleter` by hand! The `osgiaas-cli-api` module provides the
following helper classes to make it easy to implement one:

* `com.athaydes.osgiaas.cli.args.ArgsSpec` can be used to specify the options accepted by the command.
  A `CommandCompleter` is generated automatically from that specification.
* `com.athaydes.osgiaas.cli.completer.CompletionMatcher` can be used to provide auto-completion to commands which
  can take a permutation of sub-commands and arguments, similar to the `git` CLI, for example.

## Required dependencies

To create a very basic command, the only compile-time dependency needed is on the
[Felix Shell](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.apache.felix.shell%22).

To use the shell pipeline and auto-completion interfaces, also add a dependency on the `osgiaas-cli-api` module
and follow the instructions given in the previous sections.

## Tutorial

Please visit the [osgi-run Tutorial](https://sites.google.com/a/athaydes.com/renato-athaydes/posts/osgi-runtutorial-runyourjavakotlinfregecodeinosgi).

> Check one of the existing single-command modules at [modules/cli](../../modules/cli) for inspiration.
