# OSGiaaS CLI Javac Module

The OSGiaaS CLI Javac Module exports the `java` command which can run Java code statements, return the value of
simple Java expressions, or compile Java classes on the run.

## Details

All statements entered earlier are executed each time a new statement is entered.
The previous statements can be forgotten with the -r (reset) option.

The java command accepts the following flags:

  * -r: reset the current code statement buffer.
  * -ra: reset the current code statement buffer and imports.
  * -s: show the current statement buffer.
  * -c: define a class.

Simple example:

```
>> java return 2 + 2
4
```

Multi-line example to define a separate class:

```
>> :{
java -c class Person {
  String name;
  int age;
  Person(String name, int age) {
    this.name = name;
    this.age = age;
  }
  public String toString() { return "Person(" + name + "," + age + ")"; }}
:}

>> java return new Person("Mary", 24);
Person(Mary, 24)
```

When run through pipes, the Java snippet should be a `Function<String, ?>` that takes each input line as an argument,
returning something to be printed (or null).

Example:

```
> some_command | java line -> line.contains("text") ? line : null
```
