package com.athaydes.osgiaas.cli.groovy.command

import com.athaydes.osgiaas.api.stream.LineOutputStream
import com.athaydes.osgiaas.cli.CommandHelper
import com.athaydes.osgiaas.cli.StreamingCommand
import com.athaydes.osgiaas.cli.args.ArgsSpec
import groovy.transform.CompileStatic
import org.osgi.framework.BundleContext

import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

@CompileStatic
class GroovyCommand implements StreamingCommand {

    static final String RESET_CODE_ARG = "-r"
    static final String SHOW_ARG = "-s"

    static int charOf( String c ) { ( c as char ) as int }

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>()
    private final CommandHelper.CommandBreakupOptions breakupOptions =
            CommandHelper.CommandBreakupOptions.create()
                    .quoteCodes( charOf( '"' ), charOf( "'" ) )
                    .includeQuotes( true )

    final GroovyShell shell = new GroovyShell()

    final List<String> codeBuffer = [ ]
    final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( RESET_CODE_ARG )
            .accepts( SHOW_ARG )
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

           Example:

           >> groovy 2 + 2
           4

           When run through pipes, the script should return a Closure that takes each input line as an argument.

           For example:

           >> some_command | groovy { line -> println line }

           The curly braces can be omitted:

           >> some_command | groovy line -> println line

           The following options are supported:

             * $RESET_CODE_ARG: reset the current code statements buffer.
             * $SHOW_ARG: show the current statements buffer.

           The code statements buffer currently contains all entered import statements (so imports do not need to be
           re-typed on every command).

           State is maintained between invocations:

           >> groovy x = 10
           10
           >> groovy x + 1
           11
           """.stripIndent()
    }

    @Override
    OutputStream pipe( String line, PrintStream out, PrintStream err ) {
        def command = ( line.trim() - 'groovy' ).trim()

        if ( !command.startsWith( "{" ) || !command.endsWith( "}" ) ) {
            command = "{ " + command + " }"
        }

        def callback = run( command, out, err )
        if ( callback instanceof Closure ) {
            new LineOutputStream( callback as Consumer<String>, out )
        } else {
            throw new RuntimeException( 'When used in a pipeline, the groovy script must return a ' +
                    'Closure callback that takes one text line of the input at a time.\n' +
                    'Example: ... | groovy def count = 0; { line -> out.println "Line ${count++}: $line" }' )
        }
    }

    @Override
    void execute( String line, PrintStream out, PrintStream err ) {
        def command = argsSpec.parse( line, breakupOptions )
        if ( command.hasArg( RESET_CODE_ARG ) ) {
            codeBuffer.clear()
        } else if ( command.hasArg( SHOW_ARG ) ) {
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
        } catch ( Exception e ) {
            err.println( e )
        }
    }

}