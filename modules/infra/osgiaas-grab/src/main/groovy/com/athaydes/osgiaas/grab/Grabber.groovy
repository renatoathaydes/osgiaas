package com.athaydes.osgiaas.grab

import com.athaydes.osgiaas.grab.ivy.IvyModuleParser
import com.athaydes.osgiaas.grab.wrap.JarWrapper
import groovy.transform.CompileStatic

import java.util.stream.Stream

@CompileStatic
class Grabber {

    private final IvyModuleParser ivyModuleParser = new IvyModuleParser()
    private final Map<String, String> repositories

    Grabber( Map<String, String> repositories ) {
        this.repositories = repositories
    }

    /**
     * Grabs an artifact from the one of the configured repositories.
     * @param artifact to grab. Use the form "groupId:artifactId:version[:classifier]".
     * @return the result of trying to grab the artifact, if successful.
     * @throws GrabException if there was a problem grabbing the artifact
     */
    GrabResult grab( String artifact ) {
        grab( artifact, true )
    }

    /**
     * Grabs an artifact from the one of the configured repositories.
     * @param artifact to grab. Use the form "groupId:artifactId[:version[:classifier]]".
     * @param transitive include transitive dependencies.
     * @return the result of trying to grab the artifact, if successful.
     * @throws GrabException if there was a problem grabbing the artifact
     */
    GrabResult grab( String artifact, boolean transitive ) {
        def parts = artifact.trim().split( ':' )
        if ( parts.size() == 2 || parts.size() == 3 || parts.size() == 4 ) {
            def group = parts[ 0 ]
            def name = parts[ 1 ]
            def version = ( parts.size() > 2 ? parts[ 2 ] : 'latest.integration' )
            def classifier = ( parts.size() == 4 ? parts[ 3 ] : '' )

            def grapes = findGrapesHome()

            def grape = downloadAndGetLocation( grapes, group, name, version, classifier, transitive )
            def grapeLocation = grape.file as File
            def grapeVersion = grape.version as String

            if ( grapeLocation.exists() ) {
                def dependencies = transitive ?
                        collectGrapeDependencies( grapes, grapeLocation, grapeVersion ) :
                        Stream.<File> empty()

                return new GrabResult( grapeVersion, grapeLocation, dependencies )
            } else {
                throw new GrabException( "Grape was not saved in the expected location: $grapeLocation\n" )
            }
        } else {
            throw new GrabException( "Cannot understand artifact pattern: $artifact.\n" +
                    "Pattern should be: groupId:artifactId:version[:classifier]" )
        }
    }

    private String getReposString() {
        repositories.collect { name, repo ->
            "@GrabResolver(name='$name', root='$repo')"
        }.join( '\n' )
    }

    private static File findGrapesHome() {
        def grapes = ( System.getProperty( 'grape.root' ) ?:
                ( System.getProperty( 'user.home' ) + '/.groovy' ) ) + '/grapes'

        def grapesDir = new File( grapes )

        if ( !grapesDir.directory ) {
            grapesDir.mkdirs()
        }

        grapesDir
    }

    private void dowloadGrape( group, name, version, classifier = '', transitive = true ) {
        def grabInstruction = "@Grab(group='$group', module='$name', version='$version'" +
                ( transitive ? '' : ', transitive=false' ) +
                ( classifier ? ", classifier='$classifier')" : ')' )

        String script = [ getReposString(), grabInstruction, 'import java.util.List' ].join( '\n' )

        Eval.me( script )
    }

    private Stream<File> collectGrapeDependencies( File grapesDir, File grape,
                                                   String version ) {
        def ivyModule = ivyModuleLocationForGrape( grape, version )
        Stream<File> dependencies = Stream.empty()
        if ( ivyModule.canRead() ) {
            dependencies = ivyModuleParser.getDependenciesFrom( ivyModule )
                    .parallelStream().flatMap { Map dep ->
                def depGrape = downloadAndGetLocation( grapesDir, dep.group, dep.name, dep.version ).file as File
                if ( depGrape == null ) {
                    return Stream.empty()
                } else {
                    return Stream.concat( Stream.of( depGrape ),
                            collectGrapeDependencies( grapesDir, depGrape, dep.version as String ) )
                }
            }
        }

        return dependencies
    }

    private Map downloadAndGetLocation( File grapes, group, name, version, classifier = '', transitive = true ) {
        def moduleDir = new File( grapes, "$group/$name" )

        try {
            dowloadGrape( group, name, version, classifier, transitive )
            def properties = new Properties()
            new File( moduleDir, "ivydata-${version}.properties" ).withInputStream {
                properties.load( it )
            }
            def actualVersion = properties[ 'resolved.revision' ] ?: version
            def jar = new File( moduleDir, "jars/$name-${actualVersion}.jar" )
            return [ file   : JarWrapper.wrap( jar, actualVersion as String ),
                     version: actualVersion ]
        } catch ( Throwable e ) {
            throw new GrabException( "Unable to download artifact: $group:$name:$version - " +
                    // try to get only the useful part of the Exception String, which includes the whole stacktrace
                    "${e.toString().split( '\n|\r' ).take( 2 ).join( ' ' )}\n" +
                    "Make sure the artifact exists in one of the configured repositories." )
        }
    }

    private static File ivyModuleLocationForGrape( File grape, String version ) {
        File parent = grape.parentFile
        if ( parent.name == 'jars' ) {
            parent = parent.parentFile
        }
        new File( parent, "ivy-${version}.xml" )
    }

}
