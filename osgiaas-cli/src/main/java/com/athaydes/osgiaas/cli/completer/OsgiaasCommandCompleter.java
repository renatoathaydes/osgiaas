package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsgiaasCommandCompleter
        extends UsesCliProperties
        implements CommandCompleter {

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        if ( !buffer.contains( " " ) ) {
            return completeCommand( buffer, cursor, candidates );
        } else {
            return cursor;
        }
    }

    private int completeCommand( String buffer, int cursor, List<CharSequence> candidates ) {
        withCliProperties( cliProperties -> {
            String prefix = buffer.substring( 0, cursor );
            candidates.addAll( Stream.of( cliProperties.availableCommands() )
                    .filter( cmd -> cmd.startsWith( prefix ) )
                    .sorted()
                    .collect( Collectors.toList() ) );
        } );
        return 0;
    }
}
