package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.cli.util.CommandHelper;
import org.apache.felix.shell.Command;

import java.io.PrintStream;

public class GrepCommand implements Command {

    @Override
    public String getName() {
        return "grep";
    }

    @Override
    public String getUsage() {
        return "grep <regex> <text-to-search>";
    }

    @Override
    public String getShortDescription() {
        return "Shows only input text lines matching a regular expression.\n" +
                "Because it is not possible to enter multiple lines manually, this command is often used to filter " +
                "output from other commands via the '|' (pipe) operator.";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        String[] parts = CommandHelper.breakupArguments( line, 3 );
        if ( parts.length == 3 ) {
            String regex = ".*" + parts[ 1 ] + ".*";
            String text = parts[ 2 ];
            String[] textLines = text.split( "\n" );
            for (String txtLine : textLines) {
                if ( txtLine.matches( regex ) ) {
                    out.println( txtLine );
                }
            }
        } else {
            CommandHelper.printError( err, getUsage(),
                    "Wrong number of arguments provided." );
        }
    }
}
