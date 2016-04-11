package com.athaydes.osgiaas.cli.completer;

public class UseCommandCompleter extends AbstractTakesCommandsAsArgsCompleter {

    @Override
    protected String commandName() {
        return "use";
    }

}