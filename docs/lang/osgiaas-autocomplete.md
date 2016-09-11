# OSGiaaS Autocomplete Module

Very simple module providing an interface for text auto-completion implementations which exposes two different
implementations:

* `com.athaydes.osgiaas.autocomplete.Autocompleter.defaultAutocompleter()`
* `com.athaydes.osgiaas.autocomplete.Autocompleter.getStartWithAutocompleter()`

> This module may be used in non-OSGi applications as well as in OSGi-based application in the same manner.
  The implementation classes are not visible to OSGi applications and should be treated as internal by other
  Java applications.

The default implementation uses a smarter auto-completion strategy whereas capitalized letters are used as word
boundaries, allowing simpler queries for camel-cased Strings.

The `startWith` implementation is simpler: it only auto-completes text when some option *starts with*, exactly, some
text.

For example, given the following completion options:

```
toString(), toDateString(, toInteger()
```

The default implementation would give the following completions for the given examples:


| text         | completions                             |
|--------------|-----------------------------------------|
| toS          | toString()                              |
| tS           | toString()                              |
| toDS         | toDateString(                           |
| to           | toString(), toDateString(), toInteger() |

When using the `startWith` implementation:

| text         | completions                             |
|--------------|-----------------------------------------|
| toS          | toString()                              |
| tS           | -                                       |
| toDS         | -                                       |
| to           | toString(), toDateString(), toInteger() |
