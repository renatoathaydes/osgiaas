package com.athaydes.osgiaas.cli.ivy;

import com.athaydes.osgiaas.cli.CommandCompleter;

import java.util.List;

public class IvyCommandCompleter implements CommandCompleter {

    private final CommandCompleter ivyCommandCompleter = IvyCommand.argsSpec.getCommandCompleter( IvyCommand.NAME );

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        return ivyCommandCompleter.complete( buffer, cursor, candidates );
    }

}
