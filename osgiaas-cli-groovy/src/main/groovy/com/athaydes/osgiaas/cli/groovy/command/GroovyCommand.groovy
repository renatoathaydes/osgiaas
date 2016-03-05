package com.athaydes.osgiaas.cli.groovy.command

import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.StreamingCommand
import com.athaydes.osgiaas.api.stream.LineAccumulatorOutputStream
import groovy.transform.CompileStatic

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
        new LineAccumulatorOutputStream( { String fullInput ->
            run( line.trim() - 'groovy', fullInput, out, err )
        }, out )
    }

    @Override
    void execute( String line, PrintStream out, PrintStream err ) {
        def args = CommandHelper.breakupArguments( line, 2 )
        if ( args.size() != 2 ) {
            CommandHelper.printError( err, getUsage(), 'Wrong number of arguments provided.' )
        } else {
            run( args[ 1 ], '', out, err )
        }
    }

    private static void run( String script, String input, PrintStream out, PrintStream err ) {
        try {
            def result = new GroovyShell( new Binding( input: input, out: out, err: err ) )
                    .evaluate( script )
            if ( result != null ) out.println( result )
        } catch ( Exception e ) {
            err.println( e )
        }
    }

}