# OSGi-aaS CLI

A Command Line Interface (CLI) based on on [JLine](http://jline.github.io/jline2/)
and [Apache Felix Shell](http://felix.apache.org/documentation/subprojects/apache-felix-shell.html).

## Commands

Besides the OSGi Commands exported by the Felix Shell bundle (which allows inspecting and
monitoring the OSGi system itself), this bundle adds the following Commands:

* `prompt` - changes the shell prompt (to use whitespaces, quote the prompt as in `" > "`).
* `color` - set the default colors of the CLI.
* `alias` - alias a command with a different name.
* `grep` - filter lines matching a regular expression.
* `highlight` - color-highlight lines matching a regular expression.

For more information about the commands, type `help <command>` in the CLI itself.

## Command details

### prompt

To change the prompt, simply type the prompt command followed by the new prompt.

To add spaces after a prompt, you can quote the new prompt:
 
```
**>>** prompt "renato> "
**renato>** |
```

### color

The color command can change the color of:

* the **prompt**
* the commands' **text** output
* the commands' **error** output

For example, to change the prompt color to green, the text output to white, and the error output to yellow:

```
color green prompt
color white text
color yellow error
```

To change all of them at once, type just the color:

```
color cyan
```

For the color command to work, your shell must support ANSI formatting.

Available ANSI colors:

```
black, red, green, yellow, blue, purple, cyan, white
```

### alias

The alias command lets you alias existing commands.

For example, if you thing the `highlight` command is too long to type, you can alias it with `hl` as follows:

```
alias hl=highlight
```

Now, every time you type the `hl` command, the osgiaas-cli will replace it with the existing `highlight` command,
passing all the arguments to it.

To show all existing aliases, type:

```
alias show
```

To remove an existing alias, use `alias remove`:

```
alias remove hl
```

Notice that aliases can include arguments as well if you quote the command with the arguments:

```
alias hl="highlight -F red -B yellow"
```

### grep

A simple implementation of the famous grep utility.

Grep filters the input it is given, showing only lines that match the given regular expression.

It is usually used with the pipe operator.

For example, to list all installed OSGi bundles (which the `ls` command does) that contain the word "cli":

```
ps | grep cli
```

To type arbitrary regular expressions, you should quote it:

```
ps | grep "\\[Active.*\\(1\\.11\\)"
```

All regular expressions are wrapped into two `.*`, so the above regular expression would be turned into
`.*\\[Active.*\\(1\\.11\\).*`. This is done to support simple search, as in `grep Active`.

The following flags are supported:

* -B <lines> number of lines to print before each match.
* -A <lines> number of lines to print after each match.

Which can be used like this, for example:

```
ps | grep -B 4 -A 2 "\\[Active.*\\(1\\.11\\)"
```

### highlight

To search for text in the output of another command, but still show all text, you can use the `highlight` command.
Just like the grep command, highlight is mostly useful when used with the pipe operator to receive input from
other commands.

ANSI formatting is used to color-highlight text in shells that support it.

For example, to highlight all lines matching a regular expression `\\[Active.*\\(1\\.11\\)`:

```
ps | highlight "\\[Active.*\\(1\\.11\\)" 
```

The following flags are accepted:

* -B <color> background color (yellow, by default)
* -F <color[+format]*> text color and format

The accepted colors are the same as for the `color` command.

Available formats are (each item shown in the short format as well as the long format):

* `high_intensity` or `hi`
* `low_intensity` or `li`
* `italic` or `i`
* `underline` or `u`
* `blink` or `b`
* `rapid_blink` or `rb`
* `reverse_video` or `rv`
* `invisible_text` or `it`

Examples of valid invocations of the highlight command:

```
# show all available commands, highlighting all containing a 's'
help | highlight s

# show all installed bundles, highlighting those containing the 'osgi' word
# with a blue background and green, blinking text 
ps | highlight -B blue -F green+blink osgi

# show the headers of bundle with ID 10, highlighting the `Bundle-SymbolicName` entry
# with a red background and yellow, high-intensity, underlined text 
headers 10 | highlight -B red -F yellow+hi+u "Bundle-SymbolicName"
```

## Command modifier

Raw commands given by the user may be modified by OSGi services implementing the `CommandModifier` interface.

The following command modifiers, or operators, are exported by this bundle:

* `&&` - breaks up a single line into several commands.
* '|' - pipes the output of a command as the input of the next command.
* '|>' - takes the output of a command as a variable that can be replaced anywhere in the next command.

Examples:

### && operator

```
# Run the `color blue` command, then run the `color red prompt` command:
color blue && color red prompt
```

### | operator

```
# Grep all lines matching the `osgiaas.*api` regular expression from the output of the `ps` command:
ps | grep "osgiaas.*api"
```

### |> operator

The `|>` operator should be used instead of the simpler `|` operator when the output of the previous
command is part of, but not the full, or last, argument it takes.

The `$(it)` variable holder is used by default to refer to the output of the previous command.

To change that, you can use the form `prevCmd |>var_name cmd $(var_name) etc`. In this form, the output
of the previous command goes into a named variable (`var_name` in this case) which is referred to as
`$(var_name)` by the command being invoked.

This operator is mostly useful with more advanced commands.

For example, the `groovy` command (exported by `osgiaas-cli-groovy`), which executes Groovy scripts,
can make use of this operator in very interesting ways:

```
help |>cmds groovy '''$(cmds)'''.split('\n').takeWhile { it.trim().empty == false }.sort()
```

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
