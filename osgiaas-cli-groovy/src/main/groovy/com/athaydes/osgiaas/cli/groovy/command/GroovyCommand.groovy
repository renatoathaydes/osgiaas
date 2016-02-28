package com.athaydes.osgiaas.cli.groovy.command

import com.athaydes.osgiaas.api.cli.CommandHelper
import groovy.transform.CompileStatic
import org.apache.felix.shell.Command

@CompileStatic
class GroovyCommand implements Command {

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
    void execute( String line, PrintStream out, PrintStream err ) {
        def args = CommandHelper.breakupArguments( line, 2 )
        if ( args.size() != 2 ) {
            CommandHelper.printError( err, getUsage(), 'Wrong number of arguments provided.' )
        } else try {
            def result = new GroovyShell( new Binding( line: line, out: out, err: err ) )
                    .evaluate( args[ 1 ] )
            if ( result != null ) out.println( result )
        } catch ( Exception e ) {
            err.println( e )
        }
    }

}