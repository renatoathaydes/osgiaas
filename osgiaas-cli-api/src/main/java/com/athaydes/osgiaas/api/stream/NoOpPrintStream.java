package com.athaydes.osgiaas.api.stream;

import java.io.PrintStream;

/**
 * A PrintStream implementation that does not write anything.
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
