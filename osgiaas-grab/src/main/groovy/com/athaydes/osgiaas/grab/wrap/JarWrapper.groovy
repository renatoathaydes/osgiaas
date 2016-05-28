package com.athaydes.osgiaas.grab.wrap

import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.Jar

import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Wraps simple jars into OSGi bundles.
 */
class JarWrapper {


    public static final String MANIFEST_NAME = 'META-INF/MANIFEST.MF'

    static File wrap( File file, String version ) {
        if ( !file.file ) {
            return file
        }

        def input = new ZipFile( file, ZipFile.OPEN_READ )

        if ( isBundle( input ) ) {
            input.close()
            return file
        }

        def artifactName = file.name - '.jar'
        def newJar = new Jar( file )

        def analyzer = new Analyzer().with {
            jar = newJar
            bundleVersion = version
            bundleSymbolicName = artifactName
            importPackage = '*'
            exportPackage = '*'
            //config.each { k, v -> it.setProperty( k as String, v.join( ',' ) ) }
            return it
        }

        def bundle = new File( file.parentFile, artifactName + '-osgi.jar' )

        updateManifest( input, bundle, analyzer.calcManifest() )
    }

    static boolean isBundle( ZipFile input ) {
        def manifest = input.entries().find { it.name == MANIFEST_NAME } as ZipEntry

        if ( manifest ) {
            def manifestLines = input.getInputStream( manifest ).readLines()
            return manifestLines.any { it.startsWith( 'Bundle' ) }
        }

        return false
    }

    static File updateManifest( ZipFile input, File bundle, Manifest manifest ) {
        def out = new ZipOutputStream( bundle.newOutputStream() )

        try {
            out.putNextEntry( new ZipEntry( MANIFEST_NAME ) )
            manifest.write( out )

            for ( entry in input.entries() ) {
                if ( entry.name != MANIFEST_NAME ) {
                    out.putNextEntry( new ZipEntry( entry ) )
                    out.write( input.getInputStream( entry ).bytes )
                }
            }
        } finally {
            input.close()
            out.close()
        }

        return bundle
    }

}
