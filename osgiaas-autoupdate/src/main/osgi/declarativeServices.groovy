commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'autoUpdaterService', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.autoupdate.AutoUpdaterService' )
    property( name: 'service.description', value: 'OSGiaaS AutoUpdater bundle registerer service' )
    reference( name: 'autoUpdater',
            'interface': 'com.athaydes.osgiaas.api.autoupdate.AutoUpdater',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setAutoUpdater',
            'unbind': 'unsetAutoUpdater' )
    reference( name: 'logService',
            'interface': 'org.osgi.service.log.LogService',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setLogService',
            'unbind': 'unsetLogService' )
}
