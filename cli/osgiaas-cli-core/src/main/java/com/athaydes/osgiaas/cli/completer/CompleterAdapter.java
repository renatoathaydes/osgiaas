package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import jline.console.completer.Completer;

import java.util.List;
import java.util.function.Supplier;

public class CompleterAdapter implements Completer {

    private final CommandCompleter commandCompleter;
    private final Supplier<String> commandBeingUsed;

    public CompleterAdapter( CommandCompleter commandCompleter,
                             Supplier<String> commandBeingUsed ) {
        this.commandCompleter = commandCompleter;
        this.commandBeingUsed = commandBeingUsed;
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        String prefix = buffer.substring( 0, cursor );

        String cmdBeindUsed = commandBeingUsed.get();
        boolean usingSomething = !cmdBeindUsed.isEmpty();
        if ( usingSomething ) {
            buffer = cmdBeindUsed + buffer;
            cursor += cmdBeindUsed.length();
        }

        int result = commandCompleter.complete( buffer, cursor, candidates );

        if ( result < 0 ) {
            return result;
        } else {
            return result - cmdBeindUsed.length();
        }
    }

    @Override
    public String toString() {
        return "CompleterAdapter{" +
                "commandCompleter=" + commandCompleter +
                ", commandBeingUsed=" + commandBeingUsed.get() +
                '}';
    }
}
