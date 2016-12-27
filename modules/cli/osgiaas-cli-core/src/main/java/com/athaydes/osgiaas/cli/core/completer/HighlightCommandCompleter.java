package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.core.command.HighlightCommand;

import java.util.List;

public class HighlightCommandCompleter implements CommandCompleter {

    private final CommandCompleter completer = HighlightCommand.argsSpec.getCommandCompleter( "highlight" );

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        return completer.complete( buffer, cursor, candidates );
    }

}
