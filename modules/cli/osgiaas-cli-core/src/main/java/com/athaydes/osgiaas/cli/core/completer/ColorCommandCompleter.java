package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.core.command.ColorCommand;

import java.util.List;

public class ColorCommandCompleter implements CommandCompleter {


    private final CommandCompleter commandCompleter = ColorCommand.colorCommandSpec.getCommandCompleter( "color" );

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        return commandCompleter.complete( buffer, cursor, candidates );
    }

}
