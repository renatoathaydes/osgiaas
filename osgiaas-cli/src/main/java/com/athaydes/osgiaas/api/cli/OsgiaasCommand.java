package com.athaydes.osgiaas.api.cli;

import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * OSGiaaS Command.
 * <p>
 * Extends FelixShell Command to add support for streaming, which allows efficient pipes.
 */
public interface OsgiaasCommand extends Command {

    /**
     * Run this command in a pipe, returning a String consumer which can be used
     * by this command to receive text lines as input.
     *
     * @param command command invocation line. May include the first line of text input.
     * @param out     output stream
     * @param err     error stream
     */
    Consumer<String> pipe( String command, PrintStream out, PrintStream err );

}
