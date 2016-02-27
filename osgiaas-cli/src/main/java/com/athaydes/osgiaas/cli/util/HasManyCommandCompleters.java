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

    public Collection<? extends Completer> getCompleters() {
        return completers.get().values();
    }

    public final void addService( CommandCompleter service ) {
        completers.updateAndGet( completerMap -> {
            Completer completer = service::complete;
            completerMap.put( service, completer );
            addCompleter( completer );
            return new HashMap<>( completerMap );
        } );
    }

    public final void removeService( CommandCompleter service ) {
        completers.updateAndGet( completerMap -> {
            @Nullable
            Completer completer = completerMap.remove( service );
            if ( completer != null ) {
                removeCompleter( completer );
                return new HashMap<>( completerMap );
            } else {
                return completerMap;
            }
        } );
    }
}
