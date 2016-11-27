package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.core.util.UsesCliProperties;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AbstractTakesCommandsAsArgsCompleter extends UsesCliProperties
        implements CommandCompleter {

    private final Completer completer;

    public AbstractTakesCommandsAsArgsCompleter() {
        String command = commandName();
        this.completer = new Completer( command, this::matchers );
    }

    protected Stream<CompletionMatcher> matchers() {
        return Stream.of( availableCommands() )
                .filter( c -> !c.equals( commandName() ) )
                .map( c -> CompletionMatcher.nameMatcher( c ) );
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

        Completer( String command, Supplier<Stream<CompletionMatcher>> childrenSupplier ) {
            super( CompletionMatcher.nameMatcher( command, childrenSupplier ) );
        }
    }

}
