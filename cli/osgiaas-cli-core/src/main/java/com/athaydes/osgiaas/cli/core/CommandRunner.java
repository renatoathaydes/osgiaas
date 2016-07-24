package com.athaydes.osgiaas.cli.core;

import java.io.PrintStream;
import java.time.Duration;

/**
 * Shell command runner.
 */
public interface CommandRunner {
    /**
     * Runs a command immediately.
     *
     * @param command command to run
     * @param out     output stream
     * @param err     error stream
     */
    void runCommand( String command, PrintStream out, PrintStream err );

    /**
     * Runs a command when it becomes available.
     * <p>
     * If the command is found, run it immediately, otherwise wait until the command
     * is registered, then run it, or until the timeout expires.
     *
     * @param command command to run
     * @param out     output stream
     * @param err     error stream
     * @param timeout to wait until the command becomes available
     */
    void runWhenAvailable( String command, PrintStream out,
                           PrintStream err, Duration timeout );
}
