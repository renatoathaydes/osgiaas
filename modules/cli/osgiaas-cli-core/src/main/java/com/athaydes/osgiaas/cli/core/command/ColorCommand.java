package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.cli.Cli;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.core.util.UsesCli;
import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.util.List;

import static com.athaydes.osgiaas.cli.CommandHelper.printError;

/**
 * Implements the shell color command.
 */
public class ColorCommand extends UsesCli implements Command {

    public enum ColorTarget {
        PROMPT, TEXT, ERROR, ALL;

        public String getArg() {
            return name().toLowerCase();
        }
    }

    @Override
    public String getName() {
        return "color";
    }

    @Override
    public String getUsage() {
        return "color [<prompt|text|error|all>] <reset|black|red|green|yellow|blue|purple|cyan|white>";
    }

    @Override
    public String getShortDescription() {
        return "Changes CLI colors.\n\n" +
                "For example, to change the color of the prompt to blue:\n\n" +
                ">> color prompt blue\n\n" +
                "If no target is provided, all targets are set to the provided color.";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        withCli( cli -> {
            List<String> commandParts = CommandHelper.breakupArguments( line, 4 );

            String color, target;

            switch ( commandParts.size() ) {
                case 0:
                case 1:
                    printError( err, getUsage(), "Too few arguments provided" );
                    return;
                case 2:
                    color = commandParts.get( 1 );
                    target = ColorTarget.ALL.getArg();
                    break;
                case 3:
                    color = commandParts.get( 2 );
                    target = commandParts.get( 1 );
                    break;
                default:
                    printError( err, getUsage(), "Too many arguments provided" );
                    return;
            }

            setColor( err, cli, color, target );
        } );
    }

    private void setColor( PrintStream err, Cli cli, String color, String target ) {
        ColorTarget colorTarget;
        try {
            colorTarget = ColorTarget.valueOf( target.toUpperCase() );
        } catch ( IllegalArgumentException e ) {
            printError( err, getUsage(), "Invalid target: " + target );
            return;
        }

        AnsiColor ansiColor;
        try {
            ansiColor = AnsiColor.valueOf( color.toUpperCase() );
        } catch ( IllegalArgumentException e ) {
            printError( err, getUsage(), "Invalid color: " + color );
            return;
        }

        if ( colorTarget == ColorTarget.ALL || colorTarget == ColorTarget.TEXT ) {
            cli.setTextColor( ansiColor );
        }
        if ( colorTarget == ColorTarget.ALL || colorTarget == ColorTarget.PROMPT ) {
            cli.setPromptColor( ansiColor );
        }
        if ( colorTarget == ColorTarget.ALL || colorTarget == ColorTarget.ERROR ) {
            cli.setErrorColor( ansiColor );
        }
    }

}
