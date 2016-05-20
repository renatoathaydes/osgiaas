package com.athaydes.osgiaas.cli.grab

import com.athaydes.osgiaas.api.cli.CommandHelper
import com.athaydes.osgiaas.api.cli.args.ArgsSpec
import com.athaydes.osgiaas.grab.GrabException
import com.athaydes.osgiaas.grab.Grabber
import groovy.transform.CompileStatic
import org.apache.felix.shell.Command

@CompileStatic
class GrabCommand implements Command {

    static final String ADD_REPO = '--add-repo'
    static final String REMOVE_REPO = '--rm-repo'
    static final String LIST_REPOS = '--list-repos'
    static final String VERBOSE = '-v'

    final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( ADD_REPO, false, true, true )
            .accepts( REMOVE_REPO, false, true, true )
            .accepts( LIST_REPOS )
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
            Grabs a Maven/Ivy artifact from a repository.

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

                if ( argMap.containsKey( LIST_REPOS ) ) {
                    showRepos out
                } else if ( reposToAdd ) {
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

                    grab rest, out, err, grapesDir, verbose.toBoolean()
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
        def allRepos = repositories + [ default: 'https://jcenter.bintray.com/' ]
        out.println "List of repositories:"
        allRepos.each { name, repo -> out.println "  * $name: $repo" }
    }

    private void grab( String artifact, PrintStream out, PrintStream err,
                       File grapes, boolean verbose ) {
        try {
            def grapeFiles = new Grabber( repositories ).grab( artifact, grapes )
            out.println grapeFiles.collect { "file://$it" }.join( ' ' )
        } catch ( GrabException e ) {
            err.println verbose ?
                    e.message :
                    "${e.message}\nRun with the $VERBOSE option for more details."
        }
    }

}
