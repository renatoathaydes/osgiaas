package com.athaydes.osgiaas.maven.grab

import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.args.ArgsSpec
import groovy.transform.CompileStatic
import org.apache.felix.shell.Command

@CompileStatic
class MavenGrab implements Command {

    static final String ADD_REPO = '--add-repo'

    // TODO implement add-repo
    final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( "--add-repo", false, true, true )
            .build()

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
        try {
            def invocation = argsSpec.parse( line )
            def argMap = invocation.arguments
            def rest = invocation.unprocessedInput

            if ( !rest && !argMap ) {
                CommandHelper.printError( err, getUsage(), "Wrong number of arguments" )
            } else {
                def reposToAdd = argMap[ ADD_REPO ]
                if ( reposToAdd ) {
                    addRepo( reposToAdd )
                } else if ( rest ) {
                    def grapes = ( System.getProperty( 'grape.root' ) ?:
                            ( System.getProperty( 'user.home' ) + '/.groovy' ) ) + '/grapes'

                    def grapesDir = new File( grapes )

                    if ( !grapesDir.directory ) {
                        grapesDir.mkdirs()
                    }

                    grab rest, out, err, grapes
                } else {
                    CommandHelper.printError( err, getUsage(), "Wrong number of arguments" )
                }
            }
        } catch ( IllegalArgumentException e ) {
            CommandHelper.printError( err, getUsage(), e.message )
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

            def grabInstruction = "@Grab(group='$group', module='$name', version='$version'" +
                    ( classifier ? ", classifier='$classifier')" : ')' )

            try {
                Eval.me( grabInstruction + ' import java.util.List' )
            } catch ( Throwable ignore ) {
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
