package com.athaydes.osgiaas.cli.completer;

public class HelpCommandCompleter extends AbstractTakesCommandsAsArgsCompleter {

    @Override
    protected String commandName() {
        return "help";
    }
}
