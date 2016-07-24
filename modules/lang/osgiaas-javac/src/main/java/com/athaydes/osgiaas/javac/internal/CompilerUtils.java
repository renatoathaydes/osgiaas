package com.athaydes.osgiaas.javac.internal;

import javax.tools.JavaFileManager;
import java.net.URI;

public class CompilerUtils {

    public static final String JAVA_EXTENSION = ".java";

    public static String packageOf( String qualifiedClassName ) {
        int lastDotIndex = qualifiedClassName.lastIndexOf( '.' );
        if ( lastDotIndex < 0 ) {
            return "";
        } else {
            return qualifiedClassName.substring( 0, lastDotIndex );
        }
    }

    public static String simpleClassNameFrom( String qualifiedClassName ) {
        int lastDotIndex = qualifiedClassName.lastIndexOf( '.' );
        if ( lastDotIndex >= 0 && lastDotIndex <= qualifiedClassName.length() - 1 ) {
            return qualifiedClassName.substring( lastDotIndex + 1 );
        } else {
            return qualifiedClassName;
        }
    }

    public static String classNameFromPath( String resourcePath ) {
        if ( resourcePath.endsWith( ".class" ) ) {
            return resourcePath.substring( 0, resourcePath.length() - ".class".length() ).replace( "/", "." );
        } else {
            throw new RuntimeException( "Resource is not a class:" + resourcePath );
        }
    }

    public static URI uri( JavaFileManager.Location location, String packageName, String relativeName ) {
        return URI.create( String.format( "%s/%s/%s", location.getName(), packageName, relativeName ) );
    }

}
