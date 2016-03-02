package com.athaydes.osgiaas.api.cli;

import org.apache.felix.shell.Command;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * OSGiaaS Command.
 * <p>
 * Extends FelixShell Command to add support for streaming, which allows efficient pipes.
 */
public interface OsgiaasCommand extends Command {

    /**
     * Run this command in a pipe, returning a stream which can be used to write input
     * for this command.
     *
     * @param line command invocation line
     * @param out  output stream
     * @param err  error stream
     * @return stream for writing input for this command
     */
    OutputStream pipe( String line, PrintStream out, PrintStream err );

}
