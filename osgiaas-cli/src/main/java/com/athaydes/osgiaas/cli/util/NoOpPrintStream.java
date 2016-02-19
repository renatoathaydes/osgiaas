package com.athaydes.osgiaas.cli.util;

import java.io.PrintStream;

/**
 * Wraps the actual outputStream to allow customization of what happens to text sent by
 * Commands.
 */
public class NoOpPrintStream extends PrintStream {

    public NoOpPrintStream() {
        super( System.out );
    }

    @Override
    public void write( int b ) {
        // no op
    }

    @Override
    public void write( byte[] buf, int off, int len ) {
        // no op
    }

}
