commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: "http://www.osgi.org/xmlns/scr/v1.3.0",
        name: 'osgiaasStandardCli', immediate: 'true',
        activate: 'start', deactivate: 'stop' ) {
    implementation( 'class': 'com.athaydes.osgiaas.cli.StandardCli' )
    property( name: 'service.description', value: 'Standard OsgiAAS CLI Service' )
    service( scope: 'singleton' ) {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.Cli' )
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CliProperties' )
    }
    reference( name: 'shellService',
            'interface': 'org.apache.felix.shell.ShellService',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setShellService',
            'unbind': 'removeShellService' )
    reference( name: 'commandModifiers',
            'interface': 'com.athaydes.osgiaas.api.cli.CommandModifier',
            'cardinality': '0..n',
            'policy': 'dynamic',
            'bind': 'addCommandModifier',
            'unbind': 'removeCommandModifier' )
}

component( name: 'colorCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.ColorCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Color Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
    reference( name: 'osgiaasStandardCli',
            'interface': 'com.athaydes.osgiaas.api.cli.Cli',
            'cardinality': '1..1',
            'policy': 'static',
            'bind': 'setCli',
            'unbind': 'removeCli' )
}

component( name: 'promptCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.PromptCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Prompt Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
    reference( name: 'osgiaasStandardCli',
            'interface': 'com.athaydes.osgiaas.api.cli.Cli',
            'cardinality': '1..1',
            'policy': 'static',
            'bind': 'setCli',
            'unbind': 'removeCli' )
}

component( name: 'aliasCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.AliasCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Alias Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandModifier' )
    }
    reference( name: 'osgiaasStandardCli',
            'interface': 'com.athaydes.osgiaas.api.cli.CliProperties',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setCliProperties',
            'unbind': 'removeCliProperties' )
}

component( name: 'andCommandModifier', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.AndCommandModifier' )
    property( name: 'service.description', value: 'OsgiAAS Cli && CommandModifier' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandModifier' )
    }
}