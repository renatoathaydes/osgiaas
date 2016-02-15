package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.AnsiColor;
import com.athaydes.osgiaas.cli.util.UsesCli;
import org.apache.felix.shell.Command;

import java.io.PrintStream;

/**
 * Implements the shell color command.
 */
public class ColorCommand extends UsesCli implements Command {

    @Override
    public String getName() {
        return "color";
    }

    @Override
    public String getUsage() {
        return "color <color>";
    }

    @Override
    public String getShortDescription() {
        return "Change the basic color of the text in the shell";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        withCli( cli -> {
            String[] parts = line.split( " " );
            if ( parts.length == 2 ) {
                try {
                    AnsiColor color = AnsiColor.valueOf( parts[ 1 ].toUpperCase() );
                    cli.setPromptColor( color );
                } catch ( IllegalArgumentException e ) {
                    err.println( "Invalid color" );
                }
            } else {
                err.println( "Error!" );
                err.println( getUsage() );
            }
        } );
    }


}
