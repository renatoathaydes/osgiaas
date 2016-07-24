final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, name: 'grabCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.grab.GrabCommand' )
    property( name: 'service.description', value: 'OSGiaaS CLI Grab Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}

component( xmlns: SCR_NAMESPACE, name: 'grabCompleter', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.grab.GrabCompleter' )
    property( name: 'service.description', value: 'OSGiaaS CLI Grab Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.cli.CommandCompleter' )
    }
}
