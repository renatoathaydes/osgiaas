package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.cli.util.CommandHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndCommandModifier implements CommandModifier {

    @Override
    public List<String> apply( String line ) {
        if ( line.trim().isEmpty() || !line.contains( "&&" ) ) {
            return Collections.singletonList( line );
        }

        String[] parts = CommandHelper.breakupArguments( line );
        List<String> result = new ArrayList<>();
        List<String> partBuilder = new ArrayList<>();

        for (String part : parts) {
            if ( part.equals( "&&" ) ) {
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
