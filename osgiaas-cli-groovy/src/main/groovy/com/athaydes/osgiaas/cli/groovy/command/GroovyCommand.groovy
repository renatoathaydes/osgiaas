package com.athaydes.osgiaas.cli.groovy.command

import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.StreamingCommand
import com.athaydes.osgiaas.api.stream.LineOutputStream
import groovy.transform.CompileStatic

import java.util.function.Consumer

@CompileStatic
class GroovyCommand implements StreamingCommand {

    @Override
    String getName() { 'groovy' }

    @Override
    String getUsage() {
        'groovy <script>'.stripMargin()
    }

    @Override
    String getShortDescription() {
        'Executes a Groovy script.'
    }

    @Override
    OutputStream pipe( String line, PrintStream out, PrintStream err ) {
        def callback = run( line.trim() - 'groovy', out, err )
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
        def args = CommandHelper.breakupArguments( line, 2 )
        if ( args.size() != 2 ) {
            CommandHelper.printError( err, getUsage(), 'Wrong number of arguments provided.' )
        } else {
            def result = run( args[ 1 ], out, err )
            if ( result != null ) out.println( result )
        }
    }

    private static run( String script, PrintStream out, PrintStream err ) {
        try {
            return new GroovyShell( new Binding( out: out, err: err ) )
                    .evaluate( script )
        } catch ( Exception e ) {
            err.println( e )
        }
    }

}