package com.athaydes.osgiaas.wrap;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Wraps simple jars into OSGi bundles.
 */
public class JarWrapper {

    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    private final File jarFile;

    @Nullable
    private File destination;

    private String importInstructions = "*";
    private String exportInstructions = "*";

    @Nullable
    private String artifactName = null;

    public JarWrapper( File jarFile ) {
        this.jarFile = jarFile;
    }

    public void setDestination( @Nullable File destination ) {
        this.destination = destination;
    }

    public void setImportInstructions( String importInstructions ) {
        this.importInstructions = importInstructions;
    }

    public void setExportInstructions( String exportInstructions ) {
        this.exportInstructions = exportInstructions;
    }

    public void setArtifactName( @Nullable String artifactName ) {
        this.artifactName = artifactName;
    }

    public File wrap( String version ) throws Exception {
        if ( !jarFile.isFile() ) {
            throw new IllegalArgumentException( "Not a file: " + jarFile );
        }

        try ( ZipFile input = new ZipFile( jarFile, ZipFile.OPEN_READ ) ) {
            if ( isBundle( input ) ) {
                return jarFile;
            }
        }

        final String name = ( artifactName == null ?
                subtract( jarFile.getName(), ".jar" ) : artifactName );

        try ( Jar newJar = new Jar( jarFile ) ) {
            Analyzer analyzer = new Analyzer();
            analyzer.setJar( newJar );
            analyzer.setBundleVersion( version );
            analyzer.setBundleSymbolicName( name );
            analyzer.setImportPackage( importInstructions );
            analyzer.setExportPackage( exportInstructions );

            File bundle = destination == null ?
                    new File( jarFile.getParentFile(), name + "-osgi.jar" ) :
                    destination;

            Manifest manifest = analyzer.calcManifest();

            return updateManifest( newJar, bundle, manifest );
        }
    }

    public static boolean isBundle( ZipFile input ) throws IOException {
        @Nullable ZipEntry manifest = null;

        Enumeration<? extends ZipEntry> entries = input.entries();

        while ( entries.hasMoreElements() ) {
            ZipEntry entry = entries.nextElement();
            if ( MANIFEST_NAME.equals( entry.getName() ) ) {
                manifest = entry;
                break;
            }
        }

        return manifest != null &&
                new Manifest( input.getInputStream( manifest ) )
                        .getMainAttributes().containsKey( "Bundle-SymbolicName" );
    }

    private static File updateManifest( Jar newJar, File bundle, Manifest manifest )
            throws Exception {
        if ( bundle.exists() ) {
            if ( !bundle.delete() ) {
                throw new IOException( "File already exists and could not be deleted" );
            }
        }

        newJar.setManifest( manifest );
        newJar.write( bundle );

        return bundle;
    }

    private static String subtract( String text, String toSubtract ) {
        if ( text.endsWith( toSubtract ) ) {
            return text.substring( 0, text.length() - toSubtract.length() );
        } else {
            return text;
        }
    }

}
