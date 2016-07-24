package com.athaydes.osgiaas.cli.core;

import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

class Commands {

    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    private final Map<String, List<Observer>> observers = new ConcurrentHashMap<>();

    @Nullable
    Command getCommand( String name ) {
        return commands.get( name );
    }

    Set<String> getCommandNames() {
        return commands.keySet();
    }

    void addCommand( Command command ) {
        commands.put( command.getName(), command );

        evictExpiredObservers();

        @Nullable List<Observer> waiters = observers.remove( command.getName() );
        if ( waiters != null ) {
            waiters.forEach( observer -> observer.consumer.accept( command ) );
        }
    }

    void removeCommand( Command command ) {
        commands.remove( command.getName() );

        evictExpiredObservers();
    }

    void runNowOrLater( String commandName,
                        Consumer<Command> consumer,
                        Duration timeout ) {
        evictExpiredObservers();

        @Nullable Command command = getCommand( commandName );

        if ( command == null ) {
            if ( !timeout.isZero() ) {
                observers.merge( commandName,
                        singletonList( new Observer( consumer, timeout ) ),
                        Commands::concat );
            }
        } else {
            consumer.accept( command );
        }

    }

    private void evictExpiredObservers() {
        Map<String, List<Observer>> updates = new HashMap<>();
        observers.forEach( ( commandName, items ) -> {
            List<Observer> alive = items.stream()
                    .filter( Observer::isAlive )
                    .collect( Collectors.toList() );
            if ( alive.size() != items.size() ) {
                updates.put( commandName, alive );
            }
        } );

        updates.forEach( ( commandName, items ) -> {
            if ( items.isEmpty() ) {
                observers.remove( commandName );
            } else {
                observers.put( commandName, items );
            }
        } );
    }

    private static List<Observer> concat( List<Observer> l1, List<Observer> l2 ) {
        List<Observer> result = new ArrayList<>( l1.size() + l2.size() );
        result.addAll( l1 );
        result.addAll( l2 );
        return Collections.unmodifiableList( result );
    }

    private static final class Observer {
        final Consumer<Command> consumer;
        final Instant expires;

        Observer( Consumer<Command> consumer, Duration timeout ) {
            this.consumer = consumer;
            this.expires = Instant.now().plus( timeout );
        }

        boolean isAlive() {
            return !Instant.now().isAfter( expires );
        }
    }


}
