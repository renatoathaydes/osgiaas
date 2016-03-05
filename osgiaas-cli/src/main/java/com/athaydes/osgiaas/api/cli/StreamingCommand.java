package com.athaydes.osgiaas.api.cli;

import org.apache.felix.shell.Command;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * OSGiaaS streaming-capable Command.
 * <p>
 * Extends FelixShell Command to add support for streaming, which allows for efficient pipes.
 * <p>
 * If a Command implements this interface, then its pipe() method will be used during piping
 * operations, instead of {@link Command}'s execute().
 */
public interface StreamingCommand extends Command {

    /**
     * Run this command in a pipe, returning a stream which can be used
     * by this command to receive text lines as input.
     *
     * @param command command invocation line. May include the first line of text input.
     * @param out     output stream
     * @param err     error stream
     * @return stream that can be used to write to this command.
     */
    OutputStream pipe( String command, PrintStream out, PrintStream err );

}
