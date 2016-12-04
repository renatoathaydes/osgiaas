package com.athaydes.osgiaas.gradle

import com.athaydes.osgiaas.api.env.NativeCommandRunner
import spock.lang.Specification
import spock.lang.Subject

class OsgiaasGradleSpec extends Specification {

    def out = new ByteArrayOutputStream()
    def err = new ByteArrayOutputStream()

    @Subject
    def gradle = new OsgiaasGradle( 'gradle', new NativeCommandRunner(),
            new PrintStream( out ), new PrintStream( err ) )

    def cleanup() {
        gradle.commandRunner.shutdown()
    }

    def "Can copy dependencies to provided directory"() {
        given: 'An existing directory'
        def tempDir = File.createTempDir()

        when: 'We ask Gradle to copy the dependencies of a known artifact to the existing directory'
        def status = gradle.copyDependencyTo( tempDir, 'jline:jline:2.13' )

        then: 'The artifact and its dependencies are copied'
        def fileNames = tempDir.listFiles().collect { it.name }.toSet()
        fileNames == ( [ 'jline-2.13.jar', 'jansi-1.11.jar' ] as Set )

        and: 'The gradle process returns 0'
        status == 0

        cleanup: 'The temporary directory is removed'
        tempDir.deleteDir()
    }

    def "Can copy non-transitive dependencies to provided directory"() {
        given: 'An existing directory'
        def tempDir = File.createTempDir()

        when: 'We ask Gradle to copy non-transitive dependencies of a known artifact to the existing directory'
        def status = gradle.copyDependencyTo( tempDir, 'jline:jline:2.13', 'transitive = false' )

        then: 'The artifact and its dependencies are copied'
        def fileNames = tempDir.listFiles().collect { it.name }.toSet()
        fileNames == ( [ 'jline-2.13.jar' ] as Set )

        and: 'The gradle process returns 0'
        status == 0

        cleanup: 'The temporary directory is removed'
        tempDir.deleteDir()
    }

    def "Can print dependencies to provided directory"() {
        when: 'We ask Gradle to copy non-transitive dependencies of a known artifact to the existing directory'
        def status = gradle.printDependencies( 'jline:jline:2.13' )

        and: 'We collect the relevant lines that get printed out'
        def lines = out.toString().split( '\n' )
        def relevantLines = [ ]
        boolean startCollection = false
        for ( line in lines ) {
            if ( startCollection ) {
                if ( line.trim().empty ) {
                    break // done
                }

                relevantLines << line.trim()
            }
            if ( !startCollection && line.contains( 'osgiRuntime' ) ) {
                startCollection = true
            }
        }

        then: 'The relevant lines contain the expected libraries'
        relevantLines == [ '\\--- jline:jline:2.13',
                           '\\--- org.fusesource.jansi:jansi:1.11' ]

        and: 'The gradle process returns 0'
        status == 0
    }


}
