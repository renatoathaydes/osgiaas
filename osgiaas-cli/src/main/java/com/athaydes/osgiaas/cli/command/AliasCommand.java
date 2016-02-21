package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandModifier;
import com.athaydes.osgiaas.cli.util.CommandHelper;
import com.athaydes.osgiaas.cli.util.UsesCliProperties;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Arrays;
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
        return "alias [rm|show] [args]\n" +
                "  * alias <alias-name>=<aliased-command>\n" +
                "  * alias rm <aliased-command>\n" +
                "  * alias show";
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
        String[] parts = CommandHelper.breakupArguments( line );
        String[] arguments = Arrays.copyOfRange( parts, 1, parts.length );
        if ( arguments.length >= 1 ) {
            String directive = arguments[ 0 ];
            String[] directiveArgs = Arrays.copyOfRange( arguments, 1, arguments.length );
            switch ( directive ) {
                case "rm":
                    runRmCommand( out, err, directiveArgs );
                    break;
                case "show":
                    runShowCommand( out, err, directiveArgs );
                    break;
                default:
                    runSetCommand( out, err, arguments );
            }
        } else {
            CommandHelper.printError( err, getUsage(),
                    "No arguments given" );
        }
    }

    private void runRmCommand( PrintStream out, PrintStream err, String[] arguments ) {
        if ( arguments.length == 1 ) {
            @Nullable
            String aliasedCommand = aliasMap.remove( arguments[ 0 ] );
            if ( aliasedCommand == null ) {
                err.println( "Alias not found" );
            } else {
                out.println( "Ok" );
            }
        } else {
            CommandHelper.printError( err, getUsage(),
                    "Wrong number of arguments.\n" +
                            "'alias rm' takes exactly one argument." );
        }
    }

    private void runShowCommand( PrintStream out, PrintStream err, String[] arguments ) {
        if ( arguments.length == 0 ) {
            out.println( aliasMap );
        } else {
            err.println( "Wrong number of arguments.\n" +
                    "'alias show' does not take any arguments." );
        }
    }

    private void runSetCommand( PrintStream out, PrintStream err, String[] arguments ) {
        for (String argument : arguments) {
            int index = argument.indexOf( '=' );
            if ( index < 0 || index == argument.length() - 1 ) {
                CommandHelper.printError( err, getUsage(),
                        "Invalid argument: " + argument );
                return;
            } else {
                String aliasedName = argument.substring( 0, index );
                String cmdName = argument.substring( index + 1 );
                @Nullable String oldAlias = aliasMap.put( aliasedName, cmdName );
                if ( oldAlias == null ) {
                    out.println( "OK" );
                } else {
                    out.println( "Replaced alias : " + oldAlias );
                }
            }
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
