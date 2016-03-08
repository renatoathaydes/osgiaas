commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'grabCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.maven.grab.MavenGrab' )
    property( name: 'service.description', value: 'OSGiaaS Maven Grab Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}
