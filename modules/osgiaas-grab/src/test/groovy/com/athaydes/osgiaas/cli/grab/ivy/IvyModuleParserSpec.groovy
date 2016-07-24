package com.athaydes.osgiaas.cli.grab.ivy

import com.athaydes.osgiaas.grab.ivy.IvyModuleParser
import spock.lang.Specification
import spock.lang.Subject

class IvyModuleParserSpec extends Specification {

    @Subject
    final ivyModuleParser = new IvyModuleParser()

    def "Can parse Ivy module file"() {
        given: 'An Ivy module file'
        def ivyModule = getClass().getResourceAsStream( '/log4j-core-ivy-module.xml' )
        def mavenPom = getClass().getResourceAsStream( '/log4j-core-maven-pom.xml' )

        when: 'The module is parsed'
        def result = ivyModuleParser.getDependenciesFrom( ivyModule, mavenPom )

        then: 'The mandatory dependencies are correctly returned'
        result == [
                [ group: 'org.apache.logging.log4j', name: 'log4j-api', version: '2.5' ]
        ]
    }

}
