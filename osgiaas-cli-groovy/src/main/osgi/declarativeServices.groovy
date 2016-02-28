commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( name: 'groovyCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.groovy.command.GroovyCommand' )
    property( name: 'service.description', value: 'Felix Shell Groovy Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}
