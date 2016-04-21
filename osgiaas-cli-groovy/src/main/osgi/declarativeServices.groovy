commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

usingCommandReference = { ->
    reference( name: 'usingCommand',
            'interface': 'com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed',
            'cardinality': '1..1',
            'policy': 'static',
            'bind': 'setKnowsCommandBeingUsed' )
}

component( name: 'groovyCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.groovy.command.GroovyCommand' )
    property( name: 'service.description', value: 'OSGiaaS Groovy Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.StreamingCommand' )
        provide( 'interface': 'com.athaydes.osgiaas.cli.groovy.command.GroovyCommand' )
    }
}

component( name: 'groovyCompleter', immediate: true ) {
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
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
    usingCommandReference()
}
