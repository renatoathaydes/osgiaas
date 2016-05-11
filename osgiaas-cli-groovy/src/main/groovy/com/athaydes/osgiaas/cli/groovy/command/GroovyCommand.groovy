package com.athaydes.osgiaas.cli.groovy.command

import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.StreamingCommand
import com.athaydes.osgiaas.api.cli.args.ArgsSpec
import com.athaydes.osgiaas.api.stream.LineOutputStream
import groovy.transform.CompileStatic
import org.osgi.service.component.ComponentContext

import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

@CompileStatic
class GroovyCommand implements StreamingCommand {

    static final String ADD_PRE_ARG = '--pre-add'
    static final String SHOW_PRE_ARG = '--pre'
    static final String CLEAN_PRE_ARG = '--pre-clean'

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>()

    final GroovyShell shell = new GroovyShell()
    final List<String> pre = [ ]

    final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( ADD_PRE_ARG )
            .accepts( SHOW_PRE_ARG )
            .accepts( CLEAN_PRE_ARG )
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
        'groovy <script>'.stripMargin()
    }

    @Override
    String getShortDescription() {
        '''
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

           State is maintained between invocations:

           >> groovy x = 10
           10
           >> groovy x + 1
           11
           '''.stripIndent()
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
        def command = argsSpec.parse( line, CommandHelper.CommandBreakupOptions.create().includeQuotes( true ) )
        if ( command.hasArg( ADD_PRE_ARG ) ) {
            if ( command.unprocessedInput ) {
                try {
                    shell.evaluate( command.unprocessedInput )
                    pre << command.unprocessedInput
                } catch ( e ) {
                    err.println "Error: $e"
                }
            }
        } else if ( command.hasArg( CLEAN_PRE_ARG ) ) {
            pre.clear()
        } else if ( command.hasArg( SHOW_PRE_ARG ) ) {
            for ( preItem in pre ) {
                out.println( preItem )
            }
        } else {
            def args = ( pre + [ command.unprocessedInput ] ).join( '\n' )
            if ( !args ) {
                CommandHelper.printError( err, getUsage(), 'Wrong number of arguments provided.' )
            } else {
                def result = run( args, out, err )
                if ( result != null ) out.println( result )
            }
        }
    }

    void activate( ComponentContext context ) throws Exception {
        contextRef.set context
    }

    void deactivate( ComponentContext context ) throws Exception {
        contextRef.set null
    }

    private run( String script, PrintStream out, PrintStream err ) {
        try {
            shell.context.with {
                setVariable( 'out', out )
                setVariable( 'err', err )
                setVariable( 'ctx', contextRef.get() )
                setVariable( 'binding', variables )
            }

            def result = shell.evaluate( script )

            return result
        } catch ( Exception e ) {
            err.println( e )
        }
    }

}