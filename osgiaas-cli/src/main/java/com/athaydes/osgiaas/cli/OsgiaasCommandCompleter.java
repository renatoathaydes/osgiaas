package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.cli.CliProperties;
import jline.console.completer.Completer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsgiaasCommandCompleter implements Completer {

    private final CliProperties cliProperties;

    public OsgiaasCommandCompleter( CliProperties cliProperties ) {
        this.cliProperties = cliProperties;
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        if ( !buffer.contains( " " ) ) {
            return completeCommand( buffer, cursor, candidates );
        } else {
            return cursor;
        }
    }

    private int completeCommand( String buffer, int cursor, List<CharSequence> candidates ) {
        String prefix = buffer.substring( 0, cursor );
        candidates.addAll( Stream.of( cliProperties.availableCommands() )
                .filter( cmd -> cmd.startsWith( prefix ) )
                .sorted()
                .collect( Collectors.toList() ) );
        return 0;
    }
}
