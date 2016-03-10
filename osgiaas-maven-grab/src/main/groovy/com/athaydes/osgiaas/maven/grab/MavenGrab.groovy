package com.athaydes.osgiaas.maven.grab

import com.athaydes.osgiaas.api.cli.CommandHelper
import org.apache.felix.shell.Command

class MavenGrab implements Command {

    @Override
    String getName() { "grab" }

    @Override
    String getUsage() { "grab <artifact-coordinates>" }

    @Override
    String getShortDescription() {
        "Grabs a Maven artifact from a repository"
    }

    @Override
    void execute( String line, PrintStream out, PrintStream err ) {
        def invocation = CommandHelper.parseCommandInvocation( line - getName(), 2 )

        def argMap = invocation.argumentsAsMap
        println "ARGS MAP: $argMap"

        if ( invocation.unprocessedInput || argMap.isEmpty() ) {
            CommandHelper.printError( err, getUsage(), "Wrong number of arguments" )
        } else {
            def directive = argMap.keySet()[ 0 ]
            def directiveArgs = argMap[ directive ]
            if ( !directiveArgs.isEmpty() ) {
                switch ( directive ) {
                    case '--add-repo': addRepo directiveArgs
                        break
                    default:
                        err.println( "$name - Unrecognized option: $directive" )
                }
            } else {
                def grapes = ( System.getProperty( 'grape.root' ) ?:
                        ( System.getProperty( 'user.home' ) + '/.groovy' ) ) + '/grapes'

                def grapesDir = new File( grapes )

                if ( !grapesDir.directory ) {
                    grapesDir.mkdirs()
                }

                def artifact = argMap.keySet()[ 0 ]
                grab artifact, out, err, grapes
            }

        }

    }

    private static void addRepo( List<String> repos ) {
        println "Adding repos $repos"
    }

    private static void grab( String artifact, PrintStream out, PrintStream err, String grapes ) {
        def parts = artifact.trim().split( ':' )
        if ( parts.size() == 3 || parts.size() == 4 ) {
            def group = parts[ 0 ]
            def name = parts[ 1 ]
            def version = parts[ 2 ]
            def classifier = ( parts.size() == 4 ? parts[ 3 ] : '' )

            def grabInstruction = "@Grab(group='$group', module='$name', version='$version'"
            grabInstruction += ( classifier ? ", classifier='$classifier')" : ')' )

            try {
                Eval.me( grabInstruction + ' import java.util.List' )
            } catch ( Throwable e ) {
                err.println( "Unable to download artifact: $artifact" )
                return
            }

            def grapeLocation = new File( grapes, "$group/$name/jars/$name-${version}.jar" )
            if ( grapeLocation.exists() ) {
                out.println "file://$grapeLocation"
            } else {
                err.println( "Grape was not found in the expected location: $grapeLocation" )
            }
        } else {
            err.println( "Cannot understand artifact pattern: $artifact" )
        }
    }

}
