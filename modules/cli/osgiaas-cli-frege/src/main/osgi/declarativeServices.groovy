final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, name: 'fregeCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.frege.FregeCommand' )
    property( name: 'service.description', value: 'OSGiaaS Frege Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
        //    provide( 'interface': 'com.athaydes.osgiaas.cli.StreamingCommand' )
    }
}

component( xmlns: SCR_NAMESPACE, name: 'fregeCompleter', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.frege.FregeCommandCompleter' )
    property( name: 'service.description', value: 'Frege Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.cli.CommandCompleter' )
    }
}
