package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.api.ansi.AnsiColor;
import com.athaydes.osgiaas.cli.Cli;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import com.athaydes.osgiaas.cli.core.util.UsesCli;
import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.athaydes.osgiaas.cli.CommandHelper.printError;

/**
 * Implements the shell color command.
 */
public class ColorCommand extends UsesCli implements Command {

    enum ColorTarget {
        PROMPT, TEXT, ERROR, ALL;

        public String getArg() {
            return name().toLowerCase();
        }
    }

    static final String TARGET_OPTION = "-t";
    static final String COLOR_OPTION = "-c";

    public static final ArgsSpec colorCommandSpec = ArgsSpec.builder()
            .showEnumeratedArgValuesInDocumentation()
            .accepts( TARGET_OPTION, "--target" )
            .withEnumeratedArg( "target", () -> Arrays.stream( ColorTarget.values() )
                    .map( ColorTarget::getArg )
                    .collect( Collectors.toList() ) ).end()
            .accepts( COLOR_OPTION, "--color" ).withEnumeratedArg( "color", AnsiColor::colorNames )
            .mandatory().end()
            .build();

    @Override
    public String getName() {
        return "color";
    }

    @Override
    public String getUsage() {
        return "color " + colorCommandSpec.getUsage();
    }

    @Override
    public String getShortDescription() {
        return "Changes the color of text in the CLI.\n\n" +
                "The color command accepts the following options:\n\n" +
                colorCommandSpec.getDocumentation( "  " );
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        withCli( cli -> {
            try {
                CommandInvocation invocation = colorCommandSpec.parse( line );
                if ( invocation.getUnprocessedInput().isEmpty() ) {
                    String color = invocation.getFirstArgument( COLOR_OPTION );
                    String target = invocation.getOptionalFirstArgument( TARGET_OPTION )
                            .orElse( ColorTarget.ALL.getArg() );
                    setColor( err, cli, color, target );
                } else {
                    printError( err, getUsage(), "Wrong number of arguments provided." );
                }
            } catch ( IllegalArgumentException e ) {
                printError( err, getUsage(), "Invalid argument: " + e.getMessage() );
            }
        } );
    }

    private void setColor( PrintStream err, Cli cli, String color, String target ) {
        AnsiColor ansiColor;
        try {
            ansiColor = AnsiColor.valueOf( color.toUpperCase() );
        } catch ( IllegalArgumentException e ) {
            printError( err, getUsage(), "Invalid color: " + color );
            return;
        }

        ColorTarget colorTarget;
        try {
            colorTarget = ColorTarget.valueOf( target.toUpperCase() );
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
