package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.api.cli.CommandHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndCommandModifier implements CommandModifier {

    private final String commandSeparator;

    public AndCommandModifier( String commandSeparator ) {
        this.commandSeparator = commandSeparator;
    }

    public AndCommandModifier() {
        this( "&&" );
    }

    @Override
    public List<String> apply( String line ) {
        if ( line.trim().isEmpty() || !line.contains( commandSeparator ) ) {
            return Collections.singletonList( line );
        }

        String[] parts = CommandHelper.breakupArguments( line );
        List<String> result = new ArrayList<>();
        List<String> partBuilder = new ArrayList<>();

        for (String part : parts) {
            if ( part.equals( commandSeparator ) ) {
                addParts( result, partBuilder );
                partBuilder.clear();
            } else {
                partBuilder.add( part );
            }
        }
        if ( !partBuilder.isEmpty() ) {
            addParts( result, partBuilder );
        }

        return result;
    }

    private static void addParts( List<String> result, List<String> partBuilder ) {
        result.add( String.join( " ", partBuilder ) );
    }

}
