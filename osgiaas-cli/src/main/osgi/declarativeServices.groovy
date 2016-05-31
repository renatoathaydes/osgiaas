final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

cliPropertiesReference = { ->
    reference( name: 'osgiaasStandardCli',
            'interface': 'com.athaydes.osgiaas.api.cli.CliProperties',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setCliProperties',
            'unbind': 'removeCliProperties' )
}

cliReference = { ->
    reference( name: 'osgiaasStandardCli',
            'interface': 'com.athaydes.osgiaas.api.cli.Cli',
            'cardinality': '1..1',
            'policy': 'static',
            'bind': 'setCli',
            'unbind': 'removeCli' )
}

usingCommandReference = { ->
    reference( name: 'usingCommand',
            'interface': 'com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed',
            'cardinality': '0..1',
            'policy': 'dynamic',
            'bind': 'setKnowsCommandBeingUsed' )
}

component( xmlns: SCR_NAMESPACE,
        name: 'osgiaasStandardCli', immediate: true,
        activate: 'start', deactivate: 'stop' ) {
    implementation( 'class': 'com.athaydes.osgiaas.cli.StandardCli' )
    property( name: 'service.description', value: 'Standard OsgiAAS CLI Service' )
    service( scope: 'singleton' ) {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.Cli' )
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CliProperties' )
    }
    usingCommandReference()
    reference( name: 'commands',
            'interface': 'org.apache.felix.shell.Command',
            'cardinality': '0..n',
            'policy': 'dynamic',
            'bind': 'addCommand',
            'unbind': 'removeCommand' )
    reference( name: 'commandModifiers',
            'interface': 'com.athaydes.osgiaas.api.cli.CommandModifier',
            'cardinality': '0..n',
            'policy': 'dynamic',
            'bind': 'addCommandModifier',
            'unbind': 'removeCommandModifier' )
    reference( name: 'commandCompleter',
            'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter',
            'cardinality': '0..n',
            'policy': 'dynamic',
            'bind': 'addCommandCompleter',
            'unbind': 'removeCommandCompleter' )
}


component( name: 'osgiaasCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.OsgiaasCommandCompleter' )
    property( name: 'service.description', value: 'OsgiAAS Cli Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
    cliPropertiesReference()
    usingCommandReference()
}

component( name: 'inspectCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.InspectCommandCompleter' )
    property( name: 'service.description', value: 'Felix Inspect Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
}

component( name: 'colorCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.ColorCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Color Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
    cliReference()
}

component( name: 'colorCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.ColorCommandCompleter' )
    property( name: 'service.description', value: 'OsgiAAS Color Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
    usingCommandReference()
}

component( name: 'promptCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.PromptCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Prompt Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
    cliReference()
}

component( name: 'useCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.UseCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Use Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandModifier' )
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed' )
    }
}

component( name: 'useCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.UseCommandCompleter' )
    property( name: 'service.description', value: 'OsgiAAS Use Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
    cliPropertiesReference()
    usingCommandReference()
}

component( name: 'helpCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.HelpCommandCompleter' )
    property( name: 'service.description', value: 'Felix Help Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
    cliPropertiesReference()
    usingCommandReference()
}

component( name: 'aliasCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.AliasCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Alias Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandModifier' )
    }
    cliPropertiesReference()
}

component( name: 'clearCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.ClearCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Clear Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
    cliReference()
}

component( name: 'highlightCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.HighlightCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Highlight Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
    cliPropertiesReference()
}

component( name: 'highlightCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.HighlightCommandCompleter' )
    property( name: 'service.description', value: 'OsgiAAS Highlight Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
    usingCommandReference()
}

component( name: 'grepCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.GrepCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Grep Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}

component( name: 'grepCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.GrepCommandCompleter' )
    property( name: 'service.description', value: 'OsgiAAS Cli Grep Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
    usingCommandReference()
}

component( name: 'runCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.RunCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli Run Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}

component( name: 'andCommandModifier', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.AndCommandModifier' )
    property( name: 'service.description', value: 'OsgiAAS Cli && CommandModifier' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandModifier' )
    }
}

component( name: 'lrCommand', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.command.ListResourcesCommand' )
    property( name: 'service.description', value: 'OsgiAAS Cli List Resources Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}

component( name: 'lrCommandCompleter', xmlns: SCR_NAMESPACE ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.completer.ListResourceCompleter' )
    property( name: 'service.description', value: 'OsgiAAS Cli List Resources Command Completer' )
    service {
        provide( 'interface': 'com.athaydes.osgiaas.api.cli.CommandCompleter' )
    }
}
