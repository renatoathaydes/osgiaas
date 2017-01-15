package com.athaydes.osgiaas.cli;

import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * OSGiaaS streaming-capable Command.
 * <p>
 * Extends the FelixShell Command to add support for streaming, which allows for efficient pipes.
 * <p>
 * If a Command implements this interface, then its pipe() method will be used during piping
 * operations, instead of {@link Command}'s execute(), making it much more efficient in large pipelines.
 */
public interface StreamingCommand extends Command {

    /**
     * Run this command in a pipe, returning a consumer which can be used
     * to receive text lines as input from a previous command in the pipeline.
     *
     * @param command command invocation line. May include the first line of text input.
     * @param out     output stream
     * @param err     error stream
     * @return receiver of lines from the previous command in the pipeline.
     */
    Consumer<String> pipe( String command, PrintStream out, PrintStream err );

}
