package com.athaydes.osgiaas.cli.util;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import jline.console.completer.Completer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class HasManyCommandCompleters {

    private final AtomicReference<Map<CommandCompleter, Completer>> completers =
            new AtomicReference<>( new HashMap<>() );

    protected abstract void addCompleter( Completer completer );

    protected abstract void removeCompleter( Completer completer );

    public final void addService( CommandCompleter service ) {
        completers.updateAndGet( completerMap -> {
            Map<CommandCompleter, Completer> newMap = new HashMap<>( completerMap );
            Completer completer = service::complete;
            newMap.put( service, completer );
            updateCompleters( newMap.values() );
            return newMap;
        } );
    }

    public final void removeService( CommandCompleter service ) {
        completers.updateAndGet( completerMap -> {
            Map<CommandCompleter, Completer> newMap = new HashMap<>( completerMap );
            @Nullable
            Completer completer = newMap.remove( service );
            if ( completer != null ) {
                updateCompleters( newMap.values() );
            }
            return newMap;
        } );
    }

    private void updateCompleters( Collection<Completer> completers ) {
        for (Completer completer : completers) {
            removeCompleter( completer );
        }
        for (Completer completer : completers) {
            addCompleter( completer );
        }
    }
}
