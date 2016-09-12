final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, name: 'grabAutoUpdater', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.grab.autoupdate.GrabAutoUpdater' )
    property( name: 'service.description', value: 'OSGiaaS GrabAutoUpdater' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.autoupdate.AutoUpdater' )
    }
    reference( name: 'logService',
            'interface': 'org.osgi.service.log.LogService',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setLogService',
            'unbind': 'unsetLogService' )

}
