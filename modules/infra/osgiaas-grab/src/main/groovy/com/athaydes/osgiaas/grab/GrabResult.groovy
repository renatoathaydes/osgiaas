package com.athaydes.osgiaas.grab

import groovy.transform.Canonical

import java.util.stream.Stream

/**
 * Result of grabbing an artifact, if successful.
 */
@Canonical
class GrabResult {

    final String grapeVersion
    final File grapeFile
    final Stream<File> dependencies

}
