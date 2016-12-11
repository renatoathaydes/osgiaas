final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, name: 'ivyCommand', activate: 'start' ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.ivy.IvyCommand' )
    property( name: 'service.description', value: 'OSGiaaS CLI Ivy Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}
