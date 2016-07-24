package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.cli.Cli;
import com.athaydes.osgiaas.cli.core.util.UsesCli;
import org.apache.felix.shell.Command;

import java.io.PrintStream;

public class ClearCommand extends UsesCli
        implements Command {

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getUsage() {
        return "clear";
    }

    @Override
    public String getShortDescription() {
        return "Clears the terminal";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        withCli( Cli::clearScreen );
    }
}
