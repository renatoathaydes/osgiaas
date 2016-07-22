final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'javaCommand', immediate: true, xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.java.JavaCommand' )
    property( name: 'service.description', value: 'OSGiaaS Java Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
    reference( name: 'classLoaderCapabilities',
            'interface': 'com.athaydes.osgiaas.javac.ClassLoaderContext',
            'cardinality': '1..1',
            'bind': 'setClassLoaderContext',
            'target': '(service.bundle=osgiaas-cli-java)' )
}

component( name: 'javaCompleter', immediate: true, xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.java.JavaCompleter' )
    property( name: 'service.description', value: 'OSGiaaS Java Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
}

component( name: 'classLoaderCapabilities', immediate: true, xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.java.ClassLoaderCapabilities' )
    property( name: 'service.bundle', 'osgiaas-cli-java' )
    property( name: 'service.description', value: 'JavaCommand ClassLoaderCapabilities' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.javac.ClassLoaderContext' )
    }
}
