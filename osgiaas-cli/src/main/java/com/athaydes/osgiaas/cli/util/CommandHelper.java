package com.athaydes.osgiaas.cli.util;

import java.io.PrintStream;

/**
 * Created by renato on 18/02/16.
 */
public class CommandHelper {

    public static void printError( PrintStream err, String usage, String message ) {
        err.println( message );
        err.println( "Usage: " + usage );
    }

}
