package com.athaydes.osgiaas.cli.core.completer;

public class HelpCommandCompleter extends AbstractTakesCommandsAsArgsCompleter {

    @Override
    protected String commandName() {
        return "help";
    }
}
