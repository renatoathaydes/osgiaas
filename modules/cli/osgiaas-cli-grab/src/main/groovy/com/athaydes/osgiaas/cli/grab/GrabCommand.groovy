package com.athaydes.osgiaas.cli.grab

import com.athaydes.osgiaas.cli.CommandHelper
import com.athaydes.osgiaas.cli.args.ArgsSpec
import com.athaydes.osgiaas.grab.GrabException
import com.athaydes.osgiaas.grab.Grabber
import groovy.transform.CompileStatic
import org.apache.felix.shell.Command

import java.util.stream.Stream

@CompileStatic
class GrabCommand implements Command {

    static final String ADD_REPO = '--add-repo'
    static final String REMOVE_REPO = '--rm-repo'
    static final String LIST_REPOS = '--list-repos'
    static final String VERBOSE = '-v'

    final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( ADD_REPO ).allowMultiple().withArgCount( 1, 2 ).end()
            .accepts( REMOVE_REPO ).allowMultiple().withArgCount( 1 ).end()
            .accepts( LIST_REPOS ).end()
            .accepts( VERBOSE ).end()
            .build()

    private final Map<String, String> repositories = [ : ]

    @Override
    String getName() { "grab" }

    @Override
    String getUsage() { "grab <options> <artifact-coordinates> | <sub-command> <arg>" }

    @Override
    String getShortDescription() {
        """
            Grabs a Maven/Ivy artifact from a repository.

            Artifact coordinates, composed of groupId, artifactId, and version (optionally, with a classifier),
            should be joined with ':'.

            Example: grab com.google.guava:guava:19.0

            The following options are supported:

              * -v : verbose mode. Prints information about downloads.

            Grab also supports the following sub-commands:

              * $ADD_REPO [<repo-id>] <repo> : adds a repository to grab artifacts from.
              * $REMOVE_REPO <repo-id> : removes a repository.
              * $LIST_REPOS : lists existing repositories.

            If <repo-id> is not given, the repo address is also used as its ID.

            Example: grab --add-repo spring http://repo.spring.io/release
            """.stripIndent()
    }

    @Override
    void execute( String line, PrintStream out, PrintStream err ) {
        try {
            def invocation = argsSpec.parse( line )
            def argMap = invocation.arguments
            def rest = invocation.unprocessedInput

            if ( ( !rest && !argMap ) ||
                    // there is unprocessed input with sub-commands that do not allow it
                    ( rest && [ ADD_REPO, LIST_REPOS, REMOVE_REPO ].any { argMap.keySet().contains( it ) } ) ) {
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
                    def verbose = argMap.containsKey( VERBOSE ) ? 'true' : 'false'
                    System.setProperty( 'groovy.grape.report.downloads', verbose )

                    grab rest, out, err, verbose.toBoolean()
                } else {
                    CommandHelper.printError( err, getUsage(), "Wrong number of arguments" )
                }
            }
        } catch ( IllegalArgumentException e ) {
            CommandHelper.printError( err, getUsage(), e.message )
        }
    }

    private void addRepo( List<List<String>> repoArg, PrintStream out, PrintStream err ) {
        for ( List<String> repoToAdd in repoArg ) {
            // each list of arguments has size 1 or 2
            def repoId = repoToAdd.first()
            def repoUri = repoToAdd.last()
            try {
                repositories.put( repoId, new URI( repoUri ).toString() )
            } catch ( URISyntaxException e ) {
                err.println( "Unable to add $repoId: $e" )
            }
        }

        showRepos out
    }

    private void removeRepo( List<List<String>> repoArg, PrintStream out, PrintStream err ) {
        for ( String repo in repoArg.flatten() ) {
            if ( repo == 'default' ) {
                err.println "Cannot delete the default repository"
                continue
            }
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

    private void grab( String artifact, PrintStream out, PrintStream err, boolean verbose ) {
        try {
            def grabResult = new Grabber( repositories ).grab( artifact )
            def grapeFiles = Stream.concat( Stream.of( grabResult.grapeFile ), grabResult.dependencies )
            out.println grapeFiles.collect { "file://$it" }.join( ' ' )
        } catch ( GrabException e ) {
            err.println verbose ?
                    e.message :
                    "${e.message}\nRun with the $VERBOSE option for more details."
        }
    }

}
