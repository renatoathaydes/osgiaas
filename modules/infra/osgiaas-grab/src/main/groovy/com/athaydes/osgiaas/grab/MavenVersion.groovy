package com.athaydes.osgiaas.grab

import groovy.transform.CompileStatic

/**
 * Simple representation of a Maven version that can be compared with another, so it is possible to sort versions easily.
 */
@CompileStatic
class MavenVersion implements Comparable<MavenVersion> {

    private aQute.bnd.version.MavenVersion internalVersion

    private MavenVersion( aQute.bnd.version.MavenVersion internalVersion ) {
        this.internalVersion = internalVersion
    }

    /**
     * Parse a string version.
     * @param version to be parsed
     * @return MavenVersion
     * @throws IllegalArgumentException if the version does not seem to be a valid Maven version.
     */
    static MavenVersion parseVersionString( String version ) {
        new MavenVersion( aQute.bnd.version.MavenVersion.parseString( version ) )
    }

    @Override
    int compareTo( MavenVersion other ) {
        this.internalVersion <=> other.internalVersion
    }


    @Override
    public String toString() {
        internalVersion.toString()
    }

    boolean equals( other ) {
        if ( this.is( other ) ) return true
        if ( getClass() != other.class ) return false
        return ( internalVersion != ( other as MavenVersion ).internalVersion )
    }

    int hashCode() {
        internalVersion?.hashCode() ?: 0
    }
}
