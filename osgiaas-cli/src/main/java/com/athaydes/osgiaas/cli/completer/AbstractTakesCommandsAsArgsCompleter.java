package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.KnowsCommandBeingUsed;
import com.athaydes.osgiaas.api.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.api.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AbstractTakesCommandsAsArgsCompleter extends UsesCliProperties
        implements CommandCompleter {

    private final Completer completer;

    public AbstractTakesCommandsAsArgsCompleter() {
        this.completer = new Completer( commandName(), this::availableCommands );
    }

    public void setKnowsCommandBeingUsed( @Nullable KnowsCommandBeingUsed knowsCommandBeingUsed ) {
        completer.setKnowsCommandBeingUsed( knowsCommandBeingUsed );
    }

    private String[] availableCommands() {
        AtomicReference<String[]> result = new AtomicReference<>();
        withCliProperties( cliProperties -> result.set( cliProperties.availableCommands() ) );
        if ( result.get() != null ) {
            return result.get();
        } else {
            return new String[ 0 ];
        }
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        return completer.complete( buffer, cursor, candidates );
    }

    protected abstract String commandName();

    private static class Completer extends BaseCompleter {

        Completer( String command, Supplier<String[]> availableCommandsGetter ) {
            super( CompletionMatcher.nameMatcher( command,
                    () -> Stream.of( availableCommandsGetter.get() )
                            .filter( c -> !c.equals( command ) )
                            .map( c -> CompletionMatcher.nameMatcher( c ) ) ) );
        }

        @Override
        public void setKnowsCommandBeingUsed( @Nullable KnowsCommandBeingUsed knowsCommandBeingUsed ) {
            super.setKnowsCommandBeingUsed( knowsCommandBeingUsed );
        }
    }

}
