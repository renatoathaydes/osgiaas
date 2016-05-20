package com.athaydes.osgiaas.grab

import groovy.transform.CompileStatic

import java.util.stream.Stream

@CompileStatic
class Grabber {

    private final IvyModuleParser ivyModuleParser = new IvyModuleParser()
    private final Map<String, String> repositories

    Grabber( Map<String, String> repositories ) {
        this.repositories = repositories
    }

    Stream<File> grab( String artifact, File grapes ) {
        def parts = artifact.trim().split( ':' )
        if ( parts.size() == 3 || parts.size() == 4 ) {
            def group = parts[ 0 ]
            def name = parts[ 1 ]
            def version = parts[ 2 ]
            def classifier = ( parts.size() == 4 ? parts[ 3 ] : '' )

            def grapeLocation = downloadAndGetLocation( grapes, group, name, version, classifier )

            if ( grapeLocation.exists() ) {
                return collectGrapes( grapes, grapeLocation, version )
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

    private void dowloadGrape( group, name, version, classifier = '' ) {
        def grabInstruction = "@Grab(group='$group', module='$name', version='$version'" +
                ( classifier ? ", classifier='$classifier')" : ')' )

        Eval.me( [ getReposString(), grabInstruction, 'import java.util.List' ].join( '\n' ) )
    }

    private Stream<File> collectGrapes( File grapesDir, File grape,
                                        String version ) {
        def ivyModule = ivyModuleLocationForGrape( grape, version )
        Stream<File> dependencies = Stream.empty()
        if ( ivyModule.canRead() ) {
            dependencies = ivyModuleParser.getDependenciesFrom( ivyModule )
                    .parallelStream().flatMap { Map dep ->
                def depGrape = downloadAndGetLocation( grapesDir, dep.group, dep.name, dep.version )
                if ( depGrape == null ) {
                    return Stream.empty()
                } else {
                    return collectGrapes( grapesDir, depGrape, dep.version as String )
                }
            }
        }

        Stream.concat( Stream.of( grape ), dependencies )
    }

    private File downloadAndGetLocation( File grapes, group, name, version, classifier = '' ) {
        try {
            dowloadGrape( group, name, version, classifier )
            return new File( grapes, "$group/$name/jars/$name-${version}.jar" )
        } catch ( Throwable ignore ) {
            throw new GrabException( "Unable to download artifact: $group:$name:$version\n" +
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
