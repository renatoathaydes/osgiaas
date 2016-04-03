package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsgiaasCommandCompleter
        extends UsesCliProperties
        implements CommandCompleter {

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        EffectiveInput effectiveInput = resolveInputForCompletion( buffer, cursor );

        if ( !effectiveInput.input.contains( " " ) ) {
            return completeCommand( effectiveInput, candidates );
        } else {
            return -1;
        }
    }

    private int completeCommand( EffectiveInput effectiveInput, List<CharSequence> candidates ) {
        withCliProperties( cliProperties -> candidates.addAll(
                Stream.of( cliProperties.availableCommands() )
                        .filter( cmd -> cmd.startsWith( effectiveInput.input ) )
                        .sorted()
                        .collect( Collectors.toList() ) ) );
        return effectiveInput.startIndex;
    }

    private static EffectiveInput resolveInputForCompletion( String buffer, int cursor ) {
        String prefix = buffer.substring( 0, cursor );

        int breakupIndex = Math.max( prefix.lastIndexOf( '|' ), prefix.lastIndexOf( "&&" ) );

        final String input;
        final int index;

        if ( breakupIndex > 0 ) {
            int firstIndex = Math.min( breakupIndex + 1, prefix.length() - 1 );
            String rawInput = prefix.substring( firstIndex );

            // left-trim the input. We need to keep trailing spaces to know whether to complete
            input = rawInput.replaceFirst( "\\s+", "" );
            index = firstIndex + rawInput.length() - input.length();
        } else {
            input = prefix;
            index = 0;
        }

        return new EffectiveInput( input, index );
    }

    private static class EffectiveInput {
        final String input;
        final int startIndex;

        EffectiveInput( String input, int startIndex ) {
            this.input = input;
            this.startIndex = startIndex;
        }
    }
}
