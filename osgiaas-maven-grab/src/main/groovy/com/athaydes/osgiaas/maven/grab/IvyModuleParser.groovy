package com.athaydes.osgiaas.maven.grab

import groovy.transform.Immutable
import groovy.util.slurpersupport.GPathResult

import javax.annotation.Nullable

class IvyModuleParser {

    List<Map> getDependenciesFrom( InputStream ivyModuleStream, InputStream mavenPom ) {
        def ivy = new XmlSlurper().parse( ivyModuleStream )
        def maven = new XmlSlurper().parse( mavenPom )
        extractDependencies ivy, maven
    }

    List<Map> getDependenciesFrom( File ivyModuleFile ) {
        def originalFile = new File( ivyModuleFile.parentFile, ivyModuleFile.name + '.original' )
        def parser = new XmlSlurper()

        def ivyXml = parser.parse( ivyModuleFile )
        def originalXml = null

        if ( originalFile.canRead() ) {
            originalXml = parser.parse( originalFile )
        }

        extractDependencies ivyXml, originalXml
    }

    private static List<Map> extractDependencies( GPathResult ivyXml,
                                                  @Nullable GPathResult originalXml ) {
        GPathResult dependencies = ivyXml.dependencies.dependency
        def ivyModules = ivyDependencies( dependencies )

        if ( originalXml ) {
            GPathResult originalDependencies = originalXml.dependencies.dependency
            return mavenDependencies( originalDependencies, ivyModules )
        } else {
            return ivyModules.values().collect {
                [ group: it.org, name: it.name, version: it.rev ]
            }
        }
    }

    private static Map<String, IvyModule> ivyDependencies( GPathResult dependencies ) {
        dependencies.findAll {
            it.@org && it.@name && it.@rev
        }.collectEntries {
            [ ( moduleKey( it.@org.text(), it.@name.text() ) ):
                      new IvyModule( it.@org.text(), it.@name.text(), it.@rev.text() ) ]
        }
    }

    private static List<Map> mavenDependencies( GPathResult dependencies,
                                                Map<String, IvyModule> ivyModules ) {
        dependencies.findAll {
            it.groupId?.text() && it.artifactId?.text()
        }.collectMany {
            def scope = it.scope?.text() ?: 'compile'
            def optional = it.optional?.text() == 'true'

            // get the version from Ivy as Ivy resolves the best version
            def ivyModule = ivyModules[ moduleKey( it.groupId.text(), it.artifactId.text() ) ]

            if ( ivyModule && !optional && scope in [ 'compile', 'runtime' ] ) {
                [ [ group: it.groupId.text(), name: it.artifactId.text(), version: ivyModule.rev ] ]
            } else {
                [ ]
            }
        }
    }

    private static String moduleKey( org, name ) {
        "$org:$name"
    }

}

@Immutable
class IvyModule {
    String org
    String name
    String rev
}