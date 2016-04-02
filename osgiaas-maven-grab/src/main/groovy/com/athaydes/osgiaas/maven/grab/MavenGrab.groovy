package com.athaydes.osgiaas.maven.grab

import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.args.ArgsSpec
import groovy.transform.CompileStatic
import org.apache.felix.shell.Command

@CompileStatic
class MavenGrab implements Command {

    static final String ADD_REPO = '--add-repo'
    static final String REMOVE_REPO = '--rm-repo'
    static final String VERBOSE = '-v'

    final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( ADD_REPO, false, true, true )
            .accepts( REMOVE_REPO, false, true, true )
            .accepts( VERBOSE )
            .build()

    private final Map<String, String> repositories = [ : ]

    @Override
    String getName() { "grab" }

    @Override
    String getUsage() { "grab <options> <artifact-coordinates> | <command> <arg>" }

    @Override
    String getShortDescription() {
        """
            Grabs a Maven artifact from a repository.

            Artifact coordinates, composed of groupId, artifactId, and version (optionally, with a classifier),
            should be joined with ':'.

            Example: grab com.google.guava:guava:19.0

            The following options are supported:

              * -v : verbose mode. Prints information about downloads.

            Grab also supports the following commands:

              * $ADD_REPO <repo> : adds a repository to grab artifacts from.
              * $REMOVE_REPO <repo> : removes a repository.

            Example: grab --add-repo http://repo.spring.io/release
            """.stripIndent()
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
                def reposToRemove = argMap[ REMOVE_REPO ]
                if ( reposToAdd ) {
                    addRepo( reposToAdd, out, err )
                } else if ( reposToRemove ) {
                    removeRepo( reposToRemove, out, err )
                } else if ( rest ) {
                    def grapes = ( System.getProperty( 'grape.root' ) ?:
                            ( System.getProperty( 'user.home' ) + '/.groovy' ) ) + '/grapes'

                    def grapesDir = new File( grapes )

                    if ( !grapesDir.directory ) {
                        grapesDir.mkdirs()
                    }

                    def verbose = argMap.containsKey( VERBOSE ) ? 'true' : 'false'
                    System.setProperty( 'groovy.grape.report.downloads', verbose )

                    grab rest, out, err, grapes
                } else {
                    CommandHelper.printError( err, getUsage(), "Wrong number of arguments" )
                }
            }
        } catch ( IllegalArgumentException e ) {
            CommandHelper.printError( err, getUsage(), e.message )
        }
    }

    private void addRepo( List<String> repos, PrintStream out, PrintStream err ) {
        for ( repo in repos ) {
            def parts = repo.split( '=' )
            def repoId = parts[ 0 ]
            def repoUri = parts[ parts.size() > 1 ? 1 : 0 ]

            try {
                repositories.put( repoId, new URI( repoUri ).toString() )
            } catch ( URISyntaxException e ) {
                err.println( "Unable to add $repoId: $e" )
            }
        }

        showRepos out
    }

    private void removeRepo( List<String> repos, PrintStream out, PrintStream err ) {
        for ( repo in repos ) {
            def removed = repositories.remove( repo )
            if ( !removed ) {
                err.println "Repository not found: $repo"
            }
        }

        showRepos out
    }

    private void showRepos( PrintStream out ) {
        if ( repositories.isEmpty() ) {
            out.println "No custom repositories configured."
        } else {
            out.println "List of repositories:"
            repositories.each { name, repo -> out.println "  * $name: $repo" }
        }
    }

    private String getReposString() {
        repositories.collect { name, repo ->
            "@GrabResolver(name='$name', root='$repo')"
        }.join( '\n' )
    }

    private void grab( String artifact, PrintStream out, PrintStream err, String grapes ) {
        def parts = artifact.trim().split( ':' )
        if ( parts.size() == 3 || parts.size() == 4 ) {
            def group = parts[ 0 ]
            def name = parts[ 1 ]
            def version = parts[ 2 ]
            def classifier = ( parts.size() == 4 ? parts[ 3 ] : '' )

            def grabInstruction = "@Grab(group='$group', module='$name', version='$version'" +
                    ( classifier ? ", classifier='$classifier')" : ')' )

            try {
                Eval.me( [ getReposString(), grabInstruction, ' import java.util.List' ].join( '\n' ) )
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
