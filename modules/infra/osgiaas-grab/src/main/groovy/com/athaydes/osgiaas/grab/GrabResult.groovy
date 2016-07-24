package com.athaydes.osgiaas.grab

import groovy.transform.Canonical

import java.util.stream.Stream

@Canonical
class GrabResult {

    final String grapeVersion
    final File grapeFile
    final Stream<File> dependencies

}
