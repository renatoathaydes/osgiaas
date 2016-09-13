final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, name: 'autoUpdaterService', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.autoupdate.impl.AutoUpdaterService' )
    property( name: 'service.description', value: 'OSGiaaS AutoUpdater bundle registerer service' )
    reference( name: 'autoUpdater',
            'interface': 'com.athaydes.osgiaas.autoupdate.AutoUpdater',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setAutoUpdater',
            'unbind': 'unsetAutoUpdater' )
}
