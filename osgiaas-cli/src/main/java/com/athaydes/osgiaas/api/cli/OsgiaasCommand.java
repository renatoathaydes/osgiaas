package com.athaydes.osgiaas.api.cli;

import org.apache.felix.shell.Command;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * OSGiaaS Command.
 * <p>
 * Extends FelixShell Command and adds support for streaming, which allows efficient pipes.
 */
public interface OsgiaasCommand extends Command {

    OutputStream pipe( String line, PrintStream out, PrintStream err );

}
