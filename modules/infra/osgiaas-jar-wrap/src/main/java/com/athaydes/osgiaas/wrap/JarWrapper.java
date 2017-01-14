package com.athaydes.osgiaas.wrap;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;

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

    /**
     * Set the destination of the wrapped jar.
     * <p>
     * If the destination file already exists, it will be overwritten.
     * <p>
     * By default, the destination is the same as the source file, but with the "-osgi.jar" name ending.
     * <p>
     * It can be set to null to revert to the default behavior.
     *
     * @param destination where to save the generated OSGi bundle
     * @return this
     */
    public JarWrapper setDestination( @Nullable File destination ) {
        this.destination = destination;
        return this;
    }

    /**
     * Set the import instructions to provide to Bnd while wrapping the jar.
     * <p>
     * The default is "*" (ie. import all packages used by the jar).
     *
     * @param importInstructions import instructions for Bnd
     * @return this
     */
    public JarWrapper setImportInstructions( String importInstructions ) {
        this.importInstructions = importInstructions;
        return this;
    }

    /**
     * Set the export instructions to provide to Bnd while wrapping the jar.
     * <p>
     * The default is "*" (ie. export all packages contained in the jar).
     *
     * @param exportInstructions export instructions for Bnd
     * @return this
     */
    public JarWrapper setExportInstructions( String exportInstructions ) {
        this.exportInstructions = exportInstructions;
        return this;
    }

    /**
     * Set the artifact name, used both for the "Bundle-SymbolicName" manifest header and for the generated
     * bundle file name (if the destination file was not set explicitly).
     *
     * @param artifactName name of the artifact
     * @return this
     */
    public JarWrapper setArtifactName( @Nullable String artifactName ) {
        this.artifactName = artifactName;
        return this;
    }

    /**
     * Wrap the file associated with this instance of {@code JarWrapper} if it is not a bundle already.
     * <p>
     * If the source file is already a bundle, nothing is done and this method returns the original file.
     * Notice that this means the returned file is not necessarily the same as the destination file.
     *
     * @param version version to give the bundle. If this is not a valid OSGi version, it will be converted to
     *                a {@link MavenVersion} and translated to a valid OSGi version.
     * @return the bundle file
     * @throws Exception if any error occurs while reading the source file or writing the destination bundle.
     */
    public File wrap( String version ) throws Exception {
        if ( !jarFile.isFile() ) {
            throw new IllegalArgumentException( "Not a file: " + jarFile );
        }
        if ( version.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Version must not be empty" );
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
            analyzer.setBundleVersion( Version.isVersion( version ) ?
                    Version.parseVersion( version ) :
                    MavenVersion.parseString( version ).getOSGiVersion() );
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

    /**
     * Check if the given file is a OSGi bundle.
     * <p>
     * A jar file is considered a OSGi bundle if its manifest contains the mandatory header
     * {@code Bundle-SymbolicName}.
     *
     * @param input file to check
     * @return true if this file is probably a bundle, false otherwise
     * @throws IOException if a problem occurs while reading the file
     */
    public static boolean isBundle( ZipFile input ) throws IOException {
        @Nullable ZipEntry manifestEntry = null;

        Enumeration<? extends ZipEntry> entries = input.entries();

        while ( entries.hasMoreElements() ) {
            ZipEntry entry = entries.nextElement();
            if ( MANIFEST_NAME.equals( entry.getName() ) ) {
                manifestEntry = entry;
                break;
            }
        }

        return manifestEntry != null &&
                new Manifest( input.getInputStream( manifestEntry ) )
                        .getMainAttributes().getValue( "Bundle-SymbolicName" ) != null;
    }

    private static File updateManifest( Jar newJar, File bundle, Manifest manifest )
            throws Exception {
        verifyDestinationFileCanBeWritten( bundle );
        newJar.setManifest( manifest );
        newJar.write( bundle );
        return bundle;
    }

    private static void verifyDestinationFileCanBeWritten( File file ) throws IOException {
        if ( file.exists() ) {
            if ( !file.delete() ) {
                throw new IOException( "Destination file already exists and could not be overwritten" );
            }
        } else {
            @Nullable File parent = file.getParentFile();

            boolean parentDirsExist =
                    // file is top-level
                    parent == null ||
                            // parent dir exists
                            parent.isDirectory() ||
                            // we could create the parent dirs
                            file.mkdirs();

            if ( !parentDirsExist ) {
                throw new IOException( "Destination file's parent directory does not exist and could not be created" );
            }
        }
    }

    private static String subtract( String text, String toSubtract ) {
        if ( text.endsWith( toSubtract ) ) {
            return text.substring( 0, text.length() - toSubtract.length() );
        } else {
            return text;
        }
    }

}
