# OSGiaaS Java-Autocomplete Module

Java language specialization of the [OSGiaaS Autocomplete](osgiaas-autocomplete) module.

This module exposes a richer set of interfaces (with a default implementation) for auto-completion of Java code
based not only on the given text, but also on the context where the code is located.

Because of that, a Java parser is required.

A default implementation is provided based on the [java-parser](http://javaparser.org/) project.

> This module may be used in non-OSGi applications as well as in OSGi-based application in the same manner.
  The implementation classes are not visible to OSGi applications and should be treated as internal by other
  Java applications.

To get a default Java auto-completion service instance, use the following Java code:

```java
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleter;

...

JavaAutocompleter completer = JavaAutocompleter.getDefaultAutocompleter();
```

To get completions for a code fragment (without using any context or bindings):

```java
completer.completionsFor(codeFragment, Collection.emptyMap());
```

Usually, for the best auto-completion possible, you would want to provide a context (which can provide a ClassLoader
and class imports, amongst other things) and try to discover any available
variable bindings from the code fragment so that those bindings can be used for autocompletion:

```java
import com.athaydes.osgiaas.autocomplete.Autocompleter;
import com.athaydes.osgiaas.autocomplete.java.*;
...


// get an implementation of this interface
JavaAutocompleteContext context = ...

JavaAutocompleter autocompleter = JavaAutocompleter.getAutocompleter(
    Autocompleter.defaultAutocompleter(), context);

JavaStatementParser parser = JavaStatementParser.getDefaultParser();

// first parse the code fragment to extract any declared variables that can be used during auto-completion
List<String> statements = ...
Map<String, ResultType> bindings = parser.parseStatements( statements, importedClasses );

JavaAutocompleterResult result = autocompleter.completionsFor( codeFragment, bindings );
```
