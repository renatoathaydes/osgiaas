package com.athaydes.osgiaas.cli.groovy.command

import com.athaydes.osgiaas.api.stream.LineOutputStream
import com.athaydes.osgiaas.cli.CommandHelper
import com.athaydes.osgiaas.cli.StreamingCommand
import com.athaydes.osgiaas.cli.args.ArgsSpec
import org.codehaus.groovy.runtime.metaclass.MissingMethodExceptionNoStack
import org.osgi.framework.BundleContext

import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.regex.Pattern

//@CompileStatic
class GroovyCommand implements StreamingCommand {

    static final String RESET_CODE_ARG = "-r"
    static final String RESET_CODE_LONG_ARG = "--reset"
    static final String SHOW_ARG = "-s"
    static final String SHOW_LONG_ARG = "--show"

    static int charOf( String c ) { ( c as char ) as int }

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>()
    private final CommandHelper.CommandBreakupOptions breakupOptions =
            CommandHelper.CommandBreakupOptions.create()
                    .quoteCodes( charOf( '"' ), charOf( "'" ) )
                    .includeQuotes( true )

    private final Pattern closureWithArgPattern = Pattern.compile( ".*\\s+->\\s+.*" )

    final GroovyShell shell = new GroovyShell()

    final List<String> codeBuffer = [ ]
    final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( RESET_CODE_ARG, RESET_CODE_LONG_ARG ).end()
            .accepts( SHOW_ARG, SHOW_LONG_ARG ).end()
            .build()

    GroovyCommand() {
        // add pre-built variables to the context so auto-completion works from the start
        shell.context.with {
            setVariable( 'out', System.out )
            setVariable( 'err', System.err )
            setVariable( 'ctx', contextRef.get() )
            setVariable( 'binding', variables )
        }
        Thread.start {
            shell.evaluate( '"warm up"' )
        }
    }

    @Override
    String getName() { 'groovy' }

    @Override
    String getUsage() {
        'groovy <option>|<script>'.stripMargin()
    }

    @Override
    String getShortDescription() {
        """
           Executes a Groovy script.

           If the script returns a non-null value, the value is printed.

           The following options are supported:

             * $RESET_CODE_ARG, $RESET_CODE_LONG_ARG: 
               reset the current code statements buffer (see below).
             * $SHOW_ARG, $SHOW_LONG_ARG:
               show the current statements buffer.

           The code statements buffer contains all entered import statements (so imports do not need to be
           re-typed on every command).

           Simple Example:

           >> groovy 2 + 2
           < 4

           Multi-line example to define a separate class:

           >> :{
           groovy
           @groovy.transform.Canonical
           class Person {
             String name
             int age
           }
           :}
           < class Person
           >> groovy new Person("Mary", 24)
           < Person(Mary, 24)

           When run through pipes, the Groovy code should return a Function<String, ?> that takes
           each input line as an argument, returning something to be printed (or null).

           For example, to only print the lines containing the word 'text' from the output of some_command:

           >> some_command | groovy { line -> if (line.contains("text")) line }

           The curly braces can be omitted:

           >> some_command | groovy line -> if (line.contains("text")) line
           
           Even the closure argument can be omitted (use 'it' to access it), this is Groovy after all:
           
           >> some_command | groovy if (it.contains("text")) it

           State is maintained between invocations (unless 'def' is used to define a variable):

           >> groovy x = 10
           < 10
           >> groovy x + 1
           < 11
           """.stripIndent()
    }

    @Override
    OutputStream pipe( String command, PrintStream out, PrintStream err ) {
        command = ( command.trim() - 'groovy' ).trim()

        if ( !command.startsWith( "{" ) && !command.endsWith( "}" ) ) {
            if ( command.matches( closureWithArgPattern ) ) {
                command = "{ $command }"
            } else {
                command = "{ it -> $command }"
            }
        }

        def callback = run( command, out, err )
        if ( callback instanceof Closure ) {
            new LineOutputStream( { String line ->
                def result = ( callback as Closure ).call( line )
                if ( result != null ) {
                    out.println result
                }
            } as Consumer<String>, out )
        } else {
            throw new RuntimeException( 'When used in a pipeline, the groovy script must return a ' +
                    'Closure callback that takes one text line of the input at a time.\n' +
                    'Example: ... | groovy def count = 0; { line -> "Line ${count++}: $line" }' )
        }
    }

    @Override
    void execute( String line, PrintStream out, PrintStream err ) {
        def command = argsSpec.parse( line, breakupOptions )
        if ( command.hasOption( RESET_CODE_ARG ) ) {
            codeBuffer.clear()
        } else if ( command.hasOption( SHOW_ARG ) ) {
            out.println codeBuffer.join( '\n' )
        } else {
            def code = command.unprocessedInput

            if ( command.unprocessedInput.startsWith( 'import ' ) ) {
                def importedClass = command.unprocessedInput.substring( 'import '.size() )
                        .takeWhile { c -> c != ';' && c != '\n' }

                // make sure the import statement compiles
                try {
                    def importStatement = 'import ' + importedClass
                    shell.evaluate( importStatement )
                    codeBuffer << importStatement
                    code = code.substring( importStatement.size() )
                } catch ( e ) {
                    err.println "Error: $e"
                }
            }

            if ( code.trim() ) {
                def result = run( code, out, err )
                if ( result != null ) out.println( result )
            }
        }
    }

    void activate( BundleContext context ) throws Exception {
        contextRef.set context
    }

    void deactivate( BundleContext context ) throws Exception {
        contextRef.set null
    }

    def run( String script, PrintStream out, PrintStream err ) {
        def fullScript = ( codeBuffer + [ script ] ).join( '\n' )

        try {
            shell.context.with {
                setVariable( 'out', out )
                setVariable( 'err', err )
                setVariable( 'ctx', contextRef.get() )
                setVariable( 'binding', variables )
            }

            def result = shell.evaluate( fullScript )

            return result
        } catch ( MissingMethodExceptionNoStack e ) {
            // happens when a class is defined without a main method
            return e.type
        } catch ( Exception e ) {
            err.println( e )
        }
    }

}