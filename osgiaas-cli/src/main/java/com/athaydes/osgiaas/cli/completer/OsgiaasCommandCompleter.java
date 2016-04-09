package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsgiaasCommandCompleter
        extends UsesCliProperties
        implements CommandCompleter {

    private final Pattern commandSeparatorPattern;

    public OsgiaasCommandCompleter() {
        String commandSeparatorsRegex = "[" + CommandHelper.commandSeparators.stream()
                .map( codePoint -> Pattern.quote( new String( new int[]{ codePoint }, 0, 1 ) ) )
                .collect( Collectors.joining( "" ) ) + "]+";

        commandSeparatorPattern = Pattern.compile( commandSeparatorsRegex );
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        EffectiveInput effectiveInput = resolveInputForCompletion( buffer, cursor );

        if ( effectiveInput.startIndex >= 0 ) {
            return completeCommand( effectiveInput, candidates );
        } else {
            return -1;
        }
    }

    private int completeCommand( EffectiveInput effectiveInput, List<CharSequence> candidates ) {
        int originalCandidatesCount = candidates.size();

        withCliProperties( cliProperties ->
                Stream.of( cliProperties.availableCommands() )
                        .filter( cmd -> cmd.startsWith( effectiveInput.input ) )
                        .sorted()
                        .forEach( candidates::add ) );

        if ( candidates.size() > originalCandidatesCount ) {
            return effectiveInput.startIndex;
        } else {
            return -1;
        }
    }

    private EffectiveInput resolveInputForCompletion( String buffer, int cursor ) {
        String prefix = buffer.substring( 0, cursor );

        int breakupIndex = CommandHelper.lastSeparatorIndex( prefix );

        String previousNonSpace = previousNonSpace( prefix, breakupIndex );

        final String input;
        final int index;

        // if the previous non-space character is not a separator, we should not complete
        if ( !commandSeparatorPattern.matcher( previousNonSpace ).matches() ) {
            input = "";
            index = -1;
        } else if ( breakupIndex >= 0 ) {
            int firstIndex = Math.min( breakupIndex + 1, prefix.length() - 1 );
            String rawInput = prefix.substring( firstIndex );

            // left-trim the input. We need to keep trailing spaces to know whether to complete
            input = commandSeparatorPattern.matcher( rawInput ).replaceFirst( "" );
            index = firstIndex + rawInput.length() - input.length();
        } else {
            input = prefix;
            index = 0;
        }

        return new EffectiveInput( input, index );
    }

    private static String previousNonSpace( String prefix, int index ) {
        final int space = ' ';

        while ( index > 0 ) {
            int c = prefix.codePointAt( index );
            if ( c != space ) {
                return new String( new int[]{ c }, 0, 1 );
            }
            index--;
        }

        // pretend there was a previous character which was a separator
        return "|";
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
