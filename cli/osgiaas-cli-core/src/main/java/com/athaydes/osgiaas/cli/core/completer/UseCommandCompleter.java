package com.athaydes.osgiaas.cli.core.completer;

public class UseCommandCompleter extends AbstractTakesCommandsAsArgsCompleter {

    @Override
    protected String commandName() {
        return "use";
    }

}