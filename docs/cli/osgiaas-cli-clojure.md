# OSGiaaS CLI Clojure Module

A module that implements a Clojure REPL.

## Usage

Simply type `clj` followed by any Clojure code.

For example:

```
>> clj (defn double [x] (* x 2))
< #'clojure.core/double
>> clj (double 4)
< 8
```

When run through pipes, the Clojure code should return a function that takes each input line as an argument, 
returning something to be printed (nothing is printed when the function returns nil).

For example, to only print the lines containing the word 'text' from the output of some_command:

```
>> some_command | clj (fn [line] (if (.contains line "text") line nil))
```

The following variables are always available in the "user" namespace:

  * out - the command output stream.
  * err - the command error stream.
  * ctx - the Clojure command's OSGi service ComponentContext.

For example, to check the implementation class of the OSGi `ComponentContext`:

```
>> (.getClass user/ctx)
< class org.apache.felix.scr.impl.manager.ComponentContextImpl
```

To access REPL functions, you can require the REPL namespace like this:

```
>> clj (require '[clojure.repl :as repl])
```

You can then access docs, for example:

```
>> clj (repl/doc +)
```

## System requirements

The Clojure jar is expected to be available in the runtime.

With the osgi-run plugin, you can achieve that by adding this dependency to your project:

```groovy
systemLib 'org.clojure:clojure:1.8.0'
```
