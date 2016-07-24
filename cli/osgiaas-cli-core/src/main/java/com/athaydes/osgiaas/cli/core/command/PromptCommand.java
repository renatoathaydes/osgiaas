package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.core.util.UsesCli;
import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.util.List;

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
        return "Sets the CLI prompt.\n" +
                "Use quotes to enter spaces. Example: prompt \"? \"";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        withCli( cli -> {
            List<String> parts = CommandHelper.breakupArguments( line );

            if ( parts.size() != 2 ) {
                CommandHelper.printError( err, getUsage(), "Wrong number of arguments provided" );
            } else {
                String newPrompt = parts.get( 1 );
                cli.setPrompt( newPrompt );
            }
        } );
    }

}
