# OSGiaaS Project

> OSGi as a Service

The **OSGiaaS** Project aims to provide JVM modules and a CLI to make the development
of truly modular, dynamic applications easier.

Though all modules are OSGi bundles, many modules can also be used without OSGi (in standard Java applications).

For details, check the [Documentation](docs/index.md) in the docs directory.

## Main features

* Powerful CLI that can be used to run commands written in Java or any JVM language.
* CLI features auto-completion, line editing, vi/emacs modes, history, command pipeline etc.
* Language commands turn the CLI into a polyglot REPL (JavaScript, Groovy, Java, Frege).
* grab libraries from Maven repositories and install/uninstall them on the system at any time.
* hot swapping of JVM code (enabled by OSGi), so re-compiled code can be reloaded at runtime.
* the best open-source Java 8 runtime compiler available.
