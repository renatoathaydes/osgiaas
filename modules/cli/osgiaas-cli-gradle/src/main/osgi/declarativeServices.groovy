final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, deactivate: 'stop', name: 'gradleCommand' ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.gradle.GradleCommand' )
    property( name: 'service.description', value: 'OSGiaaS CLI Gradle Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}
