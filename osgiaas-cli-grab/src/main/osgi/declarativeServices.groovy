commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'grabCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.grab.GrabCommand' )
    property( name: 'service.description', value: 'OSGiaaS CLI Grab Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}

component( name: 'grabCompleter', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.grab.GrabCompleter' )
    property( name: 'service.description', value: 'OSGiaaS CLI Grab Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
}
