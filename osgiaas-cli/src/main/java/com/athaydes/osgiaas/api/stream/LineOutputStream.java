package com.athaydes.osgiaas.api.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * OutputStream that can be used to receive a line of text at a time from a writer.
 */
public final class LineOutputStream extends OutputStream {

    private static final int BUFFER_CAPACITY = 1024;

    private final Consumer<String> onLine;
    private StringBuilder builder;
    private final AutoCloseable closeWhenDone;

    public LineOutputStream( Consumer<String> onLine, AutoCloseable closeWhenDone ) {
        this.onLine = onLine;
        this.builder = new StringBuilder( BUFFER_CAPACITY );
        this.closeWhenDone = closeWhenDone;
    }

    @Override
    public void write( int b ) throws IOException {
        if ( b == '\n' ) {
            consumeLine();
        } else {
            builder.append( ( char ) b );
        }
    }

    private void consumeLine() {
        if ( builder.length() > 0 ) {
            onLine.accept( builder.toString() );
            builder = new StringBuilder( BUFFER_CAPACITY );
        }
    }

    @Override
    public void close() throws IOException {
        consumeLine();
        try {
            closeWhenDone.close();
        } catch ( Exception e ) {
            throw new IOException( e );
        }
    }

}
