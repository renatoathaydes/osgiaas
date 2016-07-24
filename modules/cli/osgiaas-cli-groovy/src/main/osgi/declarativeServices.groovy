final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, name: 'groovyCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.groovy.command.GroovyCommand' )
    property( name: 'service.description', value: 'OSGiaaS Groovy Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
        provide( 'interface': 'com.athaydes.osgiaas.cli.StreamingCommand' )
        provide( 'interface': 'com.athaydes.osgiaas.cli.groovy.command.GroovyCommand' )
    }
}

component( xmlns: SCR_NAMESPACE, name: 'groovyCompleter', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.groovy.completer.GroovyCompleter' )
    property( name: 'service.description', value: 'Groovy Command Completer' )
    reference( name: 'groovyCommand',
            'interface': 'com.athaydes.osgiaas.cli.groovy.command.GroovyCommand',
            'cardinality': '1..1',
            'policy': 'dynamic',
            'bind': 'setGroovyCommand',
            'unbind': 'unsetGroovyCommand' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.cli.CommandCompleter' )
    }
}
