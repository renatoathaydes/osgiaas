package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UseCommandCompleter extends UsesCliProperties
        implements CommandCompleter {

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        AtomicReference<Completer> completerRef = new AtomicReference<>();
        withCliProperties( cliProperties -> completerRef.updateAndGet(
                ( c ) -> new Completer( cliProperties )
        ) );
        return completerRef.get().complete( buffer, cursor, candidates );
    }

    private class Completer extends BaseCompleter {
        Completer( CliProperties cliProperties ) {
            super( CompletionMatcher.nameMatcher( "use",
                    Stream.of( cliProperties.availableCommands() )
                            .filter( c -> !c.equals( "use" ) )
                            .map( c -> CompletionMatcher.nameMatcher( c ) )
                            .collect( Collectors.toList() )
            ) );
        }
    }
}
