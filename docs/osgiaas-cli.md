# OSGi-aaS CLI

A Command Line Interface (CLI) based on on [JLine](http://jline.github.io/jline2/)
and [Apache Felix Shell](http://felix.apache.org/documentation/subprojects/apache-felix-shell.html).

## Commands

Besides the OSGi Commands exported by the Felix Shell bundle (which allows inspecting and
monitoring the OSGi system itself), this bundle adds the following Commands:

* `prompt` - changes the shell prompt (to use whitespaces, quote the prompt as in `" > "`).
* `color` - set the default colors of the CLI.

For more information about the commands, type `help <command>` in the CLI itself.

## Command history

Command history is supported by JLine (see the JLine docs for configuring it).

Persistent history is saved on the `<user.home>/.osgiaas_cli_history` file by default.
To change the location of this file, set the `osgiaas.cli.history` System property
(with a -D option) when starting the JVM.
