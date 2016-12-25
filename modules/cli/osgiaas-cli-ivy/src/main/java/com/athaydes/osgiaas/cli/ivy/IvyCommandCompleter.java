package com.athaydes.osgiaas.cli.ivy;

import com.athaydes.osgiaas.cli.CommandCompleter;

import java.util.List;

public class IvyCommandCompleter implements CommandCompleter {

    private final CommandCompleter ivyCommandCompleter = IvyCommand.argsSpec.getCommandCompleter();

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        if ( buffer.substring( 0, cursor ).startsWith( "ivy " ) ) {
            return ivyCommandCompleter.complete( buffer, cursor, candidates );
        } else {
            return -1;
        }
    }
}
