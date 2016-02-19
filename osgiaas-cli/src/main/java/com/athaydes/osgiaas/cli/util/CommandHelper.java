package com.athaydes.osgiaas.cli.util;

import java.io.PrintStream;

public class CommandHelper {

    public static void printError( PrintStream err, String usage, String message ) {
        err.println( message );
        err.println( "Usage: " + usage );
    }

    public static String[] breakupArguments( String arguments ) {
        // TODO handle quoted arguments
        return arguments.split( " " );
    }

}
