package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandModifier;

import java.util.ArrayList;
import java.util.List;

public class AndCommandModifier implements CommandModifier {

    private static final int separatorCode = '&';
    private static final String separator = "&&";

    private static final CommandHelper.CommandBreakupOptions breakupOptions =
            CommandHelper.CommandBreakupOptions.create()
                    .includeQuotes( true )
                    .separatorCode( separatorCode )
                    .includeSeparators( true );

    @Override
    public List<String> apply( String line ) {
        List<String> result = new ArrayList<>( 2 );

        StringBuilder currentPart = new StringBuilder();

        CommandHelper.breakupArguments( line, part -> {
            if ( part.equals( separator ) ) {
                result.add( currentPart.toString().trim() );
                currentPart.delete( 0, currentPart.length() );
            } else {
                currentPart.append( part );
            }

            return true;
        }, breakupOptions );

        if ( currentPart.length() > 0 ) {
            result.add( currentPart.toString().trim() );
        }

        return result;
    }

}
