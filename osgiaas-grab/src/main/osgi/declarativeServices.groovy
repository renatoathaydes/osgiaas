commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'grabAutoUpdater', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.grab.autoupdate.GrabAutoUpdater' )
    property( name: 'service.description', value: 'OSGiaaS GrabAutoUpdater' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.autoupdate.AutoUpdater' )
    }
    reference( name: 'logService',
            'interface': 'org.osgi.service.log.LogService',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setLogService',
            'unbind': 'unsetLogService' )

}
