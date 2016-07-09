commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'javaCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.java.JavaCommand' )
    property( name: 'service.description', value: 'OSGiaaS Java Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}
