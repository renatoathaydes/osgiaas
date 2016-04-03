package com.athaydes.osgiaas.cli.completer;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.cli.CommandCompleter;
import com.athaydes.osgiaas.api.cli.CommandHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColorCommandCompleter implements CommandCompleter {

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        if ( buffer.startsWith( "color " ) && cursor >= "color ".length() ) {
            String prefix = buffer.substring( 0, cursor );
            List<String> completions = completionsFor( prefix );
            candidates.addAll( completions );
            if ( !completions.isEmpty() ) {
                return prefix.lastIndexOf( ' ' ) + 1;
            }
        }
        return -1;
    }

    private List<String> completionsFor( String prefix ) {
        List<String> parts = CommandHelper.breakupArguments( prefix );
        boolean findFullValue = prefix.endsWith( " " );
        if ( parts.size() == 1 ) {
            return Stream.of( AnsiColor.values() )
                    .map( AnsiColor::name )
                    .filter( color -> !color.startsWith( "_" ) )
                    .map( String::toLowerCase )
                    .sorted()
                    .collect( Collectors.toList() );
        }
        if ( parts.size() == 2 && !findFullValue ) {
            String colorPrefix = parts.get( 1 ).toUpperCase();
            return Stream.of( AnsiColor.values() )
                    .map( AnsiColor::name )
                    .filter( color -> color.startsWith( colorPrefix ) )
                    .map( String::toLowerCase )
                    .sorted()
                    .collect( Collectors.toList() );
        }
        if ( parts.size() >= 2 && AnsiColor.isColor( parts.get( 1 ).toUpperCase() ) ) {
            if ( ( parts.size() == 2 && findFullValue ) || ( parts.size() == 3 && !findFullValue ) ) {
                String optionPrefix = parts.size() == 3 ? parts.get( 2 ) : "";
                return Stream.of( "prompt", "text", "error" )
                        .filter( option -> option.startsWith( optionPrefix ) )
                        .collect( Collectors.toList() );
            }
        }

        return Collections.emptyList();
    }

}
