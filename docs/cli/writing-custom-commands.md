# Writing Custom Commands

## Basics

The OSGiaaS-CLI uses the [Apache Felix Shell](http://felix.apache.org/documentation/subprojects/apache-felix-shell.html)
system to discover and run commands.

So, all you need to create a basic command is the following:

* write an implementation of the `org.apache.felix.shell.Command` interface.
* publish this implementation as a OSGi service providing the `org.apache.felix.shell.Command` interface.

## Supporting shell pipelines

To get your command implementation to support efficient pipes, you also need to:

* make your command also implement the `com.athaydes.osgiaas.cli.StreamingCommand` interface.
* publish this implementation as a OSGi service that also provides the `com.athaydes.osgiaas.cli.StreamingCommand`
  interface.

> If your Command does not provide the `StreamingCommand` interface, it will still work in pipes but it will probably
  be less efficient than it should be as it will have to wait for the previous command in the pipeline to finish execution
  completely, so that it can run as if invoked with the full output of the previous command as its input.
  By implementing `StreamingCommand`, your command can accept a line of input at a time, unclogging the pipeline.

## Supporting auto-completion

For your command to get auto-completion support, you need to:

* write an implementation of the `com.athaydes.osgiaas.cli.CommandCompleter` interface.
* publish this implementation as a OSGi service providing the `com.athaydes.osgiaas.cli.CommandCompleter` interface.

## Required dependencies

To create a very basic command, the only compile-time dependency needed is on the
[Felix Shell](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.apache.felix.shell%22).

To use the shell pipeline and auto-completion interfaces, also add a dependency on the osgiaas-cli-api module
and follow the instructions given in the previous sections.

## Tutorial

The following sections explain how to create a custom command using Gradle and the `osgi-run` plugin.

TBD

> Check one of the existing single-command modules at [modules/cli](../../modules/cli) for inspiration.
