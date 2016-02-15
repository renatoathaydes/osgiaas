package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.cli.util.UsesCli;
import org.apache.felix.shell.Command;

import java.io.PrintStream;

/**
 * The prompt command can be used to change the CLI prompt.
 */
public class PromptCommand extends UsesCli implements Command {

    @Override
    public String getName() {
        return "prompt";
    }

    @Override
    public String getUsage() {
        return "prompt <new-prompt>";
    }

    @Override
    public String getShortDescription() {
        return "Modified the CLI prompt";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        withCli( cli -> {
            String command = line.trim();

            if ( !command.contains( " " ) ) {
                err.println( "Error: no argument provided." );
                err.println( getUsage() );
            } else {
                String newPrompt = command.substring( getName().length() );
                newPrompt = newPrompt.trim();
                if ( newPrompt.startsWith( "\"" ) ) {
                    newPrompt = newPrompt.substring( 1 );
                    int index = newPrompt.indexOf( '"' );
                    if ( index >= 0 ) {
                        newPrompt = newPrompt.substring( 0, index );
                    }
                }
                cli.setPrompt( newPrompt );
            }
        } );
    }

}
