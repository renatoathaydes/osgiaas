# OSGiaaS Scala CLI Module

> Status: alpha

A module that implements a Scala REPL.

## Usage

Simply type `scala` followed by any Scala code.

For example:

```
>> scala val double = (x: Int) => x * 2
< $line6.$read$$iw$$iw$$Lambda$1183/2050910608@2a003a3e
>> scala double(4)
< 8
```

When run through pipes, the Scala code should return a `Function[String, Any]`,
returning something to be printed (nothing is printed when the function returns null).

For example, to only print the lines containing the word 'text' from the output of some_command:

```
>> some_command | scala (line: String) => if (line.contains("text")) line else null
```

## System requirements

Add the following system lib to your OSGi runtime:

```
systemLib 'org.scala-lang:scala-compiler:2.12.1'
```
