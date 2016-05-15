commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'jsCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.js.command.JSCommand' )
    property( name: 'service.description', value: 'OSGiaaS JavaScript Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}
