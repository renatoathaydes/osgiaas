package com.athaydes.osgiaas.cli;

import java.io.PrintStream;

/**
 * Shell command runner.
 */
@FunctionalInterface
public interface CommandRunner {
    void runCommand( String command, PrintStream out, PrintStream err );
}
