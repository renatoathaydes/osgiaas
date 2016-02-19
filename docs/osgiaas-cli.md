# OSGi-aaS CLI

A Command Line Interface (CLI) based on on [JLine](http://jline.github.io/jline2/)
and [Apache Felix Shell](http://felix.apache.org/documentation/subprojects/apache-felix-shell.html).

## Commands

Besides the OSGi Commands exported by the Felix Shell bundle (which allows inspecting and
monitoring the OSGi system itself), this bundle adds the following Commands:

* `prompt` - changes the shell prompt (to use whitespaces, quote the prompt as in `" > "`).
* `color` - set the default colors of the CLI.
* `alias` - alias a command with a different name.

For more information about the commands, type `help <command>` in the CLI itself.

## Command history

Command history is supported by JLine (see the JLine docs for configuring it).

Persistent history is saved on the `<user.home>/.osgiaas_cli_history` file by default.
To change the location of this file, set the `osgiaas.cli.history` System property
(with a -D option) when starting the JVM.

## Init file

If you want certain commands to be emitted every time you start the CLI, you can create a
file at `<user.home>/.osgiaas_cli_init` containing all the commands you want to run, one
in each line.

The location of this file can be configured using the `osgiaas.cli.init` System property.

Example `.osgiaas_cli_init` file:

```
prompt "? "
color red prompt
color yellow error
alias exit=shutdown
```

## Sample OSGi environment

A Gradle script for setting up an OSGi environment quickly and easily with
[osgi-run](https://github.com/renatoathaydes/osgiaas-run)
can be found in the [samples directory](../samples/osgiaas-cli.gradle).

You can just copy the contents of this file into a `build.gradle` file and create the
OSGi environment with `gradle createOsgiRuntime`, then run with `bash build/osgi/run.sh` or
`build/osgi/run.bat` in Windows.
