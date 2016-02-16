package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The alias command can be used to alias other commands.
 */
public class AliasCommand extends UsesCliProperties
        implements Command, CommandModifier {

    private final Map<String, String> aliasMap = new HashMap<>();

    @Override
    public String getName() {
        return "alias";
    }

    @Override
    public String getUsage() {
        return "alias <cmd-name> <aliased-name>";
    }

    @Override
    public String getShortDescription() {
        return "Aliases a command.";
    }

    void addAliases( Map<String, String> aliases ) {
        aliasMap.putAll( aliases );
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        String[] parts = line.split( " " );
        if ( parts.length == 3 ) {
            String aliasedName = parts[ 1 ];
            String cmdName = parts[ 2 ];

            @Nullable String oldAlias = aliasMap.put( cmdName, aliasedName );
            if ( oldAlias == null ) {
                out.println( "OK" );
            } else {
                out.println( "Replaced alias : " + oldAlias );
            }
        } else {
            err.println( "Wrong number of arguments provided." );
            err.println( "Usage: " + getUsage() );
        }
    }

    @Override
    public List<String> apply( final String command ) {
        String commandName = command;
        int index = command.indexOf( ' ' );
        if ( index >= 0 ) {
            commandName = command.substring( 0, index );
        }
        @Nullable String aliasCommand = aliasMap.get( commandName );
        if ( aliasCommand != null ) {
            if ( index >= 0 ) {
                String arguments = command.substring( index, command.length() );
                aliasCommand += arguments;
            }
            return Collections.singletonList( aliasCommand );
        } else {
            return Collections.singletonList( command );
        }
    }

}
