package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.CommandModifier;
import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

public class UseCommand implements Command, CommandModifier {

    private volatile String using = "";

    @Override
    public String getName() {
        return "use";
    }

    @Override
    public String getUsage() {
        return "use <command>";
    }

    @Override
    public String getShortDescription() {
        return "Use a command.\n" +
                "\n" +
                "For example:\n" +
                "\n" +
                ">> use headers\n" +
                ">> 10\n" +
                "<output of typing 'headers 10'>\n" +
                "\n" +
                "To stop using a command, simply type 'use'.\n" +
                "\n" +
                "To skip using the selected command, start the command with a underscore ('_').\n" +
                "\n" +
                ">> use headers\n" +
                ">> _ps\n" +
                "<output of typing 'ps'>\n";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        List<String> parts = CommandHelper.breakupArguments( line, 2 );

        if ( parts.size() == 0 || parts.size() > 2 ) {
            CommandHelper.printError( err, getUsage(), "Wrong number of arguments provided" );
        } else {
            String command = parts.size() == 1 ? "" : parts.get( 1 );
            if ( command.isEmpty() && !using.isEmpty() ) {
                out.printf( "Stopped using '%s'\n", using );
            } else if ( !command.isEmpty() ) {
                out.printf( "Using '%s'\n", command );
            }

            using = command;
        }
    }

    @Override
    public List<String> apply( String line ) {
        if ( line.startsWith( "use" ) || using.isEmpty() ) {
            return Collections.singletonList( line );
        } else if ( line.startsWith( "_" ) ) {
            return Collections.singletonList( line.substring( 1 ) );
        } else {
            return Collections.singletonList( String.format( "%s %s", using, line ) );
        }
    }

    /**
     * @return the priority of this CommandModifier (20).
     */
    @Override
    public int getPriority() {
        return 20;
    }

}
