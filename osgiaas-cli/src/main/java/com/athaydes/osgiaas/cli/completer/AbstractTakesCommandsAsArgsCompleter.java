package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CliProperties;
import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed;
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractTakesCommandsAsArgsCompleter extends UsesCliProperties
        implements CommandCompleter {

    @Nullable
    private KnowsCommandBeingUsed knowsCommandBeingUsed = null;

    public void setKnowsCommandBeingUsed( @Nullable KnowsCommandBeingUsed knowsCommandBeingUsed ) {
        this.knowsCommandBeingUsed = knowsCommandBeingUsed;
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        AtomicReference<Completer> completerRef = new AtomicReference<>();
        withCliProperties( cliProperties -> completerRef.updateAndGet(
                ( c ) -> {
                    Completer completer = new Completer( cliProperties, commandName() );
                    completer.setKnowsCommandBeingUsed( knowsCommandBeingUsed );
                    return completer;
                }
        ) );
        Completer completer = completerRef.get();
        if ( completer != null ) {
            return completerRef.get().complete( buffer, cursor, candidates );
        } else {
            return -1;
        }
    }

    protected abstract String commandName();

    private static class Completer extends BaseCompleter {

        Completer( CliProperties cliProperties, String commandName ) {
            super( CompletionMatcher.nameMatcher( commandName,
                    Stream.of( cliProperties.availableCommands() )
                            .filter( c -> !c.equals( commandName ) )
                            .map( c -> CompletionMatcher.nameMatcher( c ) )
                            .collect( Collectors.toList() )
            ) );
        }
    }

}
