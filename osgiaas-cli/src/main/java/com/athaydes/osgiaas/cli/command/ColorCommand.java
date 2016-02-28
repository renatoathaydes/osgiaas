package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.api.cli.Cli;
import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.cli.util.UsesCli;
import org.apache.felix.shell.Command;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Arrays;

import static com.athaydes.osgiaas.api.cli.CommandHelper.printError;

/**
 * Implements the shell color command.
 */
public class ColorCommand extends UsesCli implements Command {

    private enum ColorTarget {
        PROMPT, TEXT, ERROR, ALL
    }

    @Override
    public String getName() {
        return "color";
    }

    @Override
    public String getUsage() {
        return "color <color> [prompt|text|error]";
    }

    @Override
    public String getShortDescription() {
        return "Changes the color of text in the shell.\n" +
                "The first argument is the color to set.\n" +
                "The second, optional argument, may limit the color change to one of " +
                "[prompt|text|error].\n" +
                "If the second argument is omitted, all colors are changed.";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        withCli( cli -> {
            String[] parts = CommandHelper.breakupArguments( line );
            String[] arguments = Arrays.copyOfRange( parts, 1, parts.length );
            if ( arguments.length == 1 ) {
                setColor( err, cli, arguments[ 0 ], null );
            } else if ( arguments.length == 2 ) {
                String color = arguments[ 0 ];
                String target = arguments[ 1 ];
                setColor( err, cli, color, target );
            } else {
                printError( err, getUsage(), "Wrong number of arguments provided." );
            }
        } );
    }

    private void setColor( PrintStream err, Cli cli, String color,
                           @Nullable String target ) {
        AnsiColor ansiColor;
        try {
            ansiColor = AnsiColor.valueOf( color.toUpperCase() );
        } catch ( IllegalArgumentException e ) {
            printError( err, getUsage(), "Invalid color: " + color );
            return;
        }

        ColorTarget colorTarget;
        try {
            colorTarget = target == null ?
                    ColorTarget.ALL :
                    ColorTarget.valueOf( target.toUpperCase() );
        } catch ( IllegalArgumentException e ) {
            printError( err, getUsage(), "Invalid target: " + color );
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
