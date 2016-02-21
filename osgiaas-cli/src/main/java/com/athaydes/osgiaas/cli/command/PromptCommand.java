package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.cli.util.CommandHelper;
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
            String[] parts = CommandHelper.breakupArguments( line );

            if ( parts.length != 2 ) {
                CommandHelper.printError( err, getUsage(), "Wrong number of arguments provided" );
            } else {
                String newPrompt = parts[ 1 ];
                cli.setPrompt( newPrompt );
            }
        } );
    }

}
