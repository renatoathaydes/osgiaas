package com.athaydes.osgiaas.grab.sample;

import com.athaydes.osgiaas.grab.GrabException;
import com.athaydes.osgiaas.grab.GrabResult;
import com.athaydes.osgiaas.grab.Grabber;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a sample class for using the {@code com.athaydes.osgiaas.grab.Grabber} service.
 */
public class GrabberSample {

    public static void main( String[] args ) {
        String artifact = "org.apache.pdfbox:pdfbox:1.8.6";
        grabArtifactFromMavenCentralAndShowItsContents( artifact );
    }

    public static void grabArtifactFromMavenCentralAndShowItsContents( String artifact ) {
        Map<String, String> repositories = new HashMap<>( 1 );
        repositories.put( "Maven Central", "https://repo1.maven.org/maven2" );

        Grabber grabber = new Grabber( repositories );

        try {
            GrabResult result = grabber.grab( artifact );
            showArtifact( result );
        } catch ( GrabException e ) {
            System.err.printf( "Problem grabbing artifact %s: %s", artifact, e.getMessage() );
        }
    }

    private static void showArtifact( GrabResult result ) {
        System.out.println( "Artifact name:    " + result.getGrapeFile() );
        System.out.println( "Artifact version: " + result.getGrapeVersion() );
        System.out.println( "Dependencies:     " + result.getDependencies()
                .map( File::getName )
                .reduce( ( acc, name ) -> String.join( ", ", acc, name ) ) );
    }

}
