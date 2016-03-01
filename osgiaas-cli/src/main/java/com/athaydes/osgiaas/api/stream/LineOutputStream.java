package com.athaydes.osgiaas.api.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * OutputStream that receives a line of text at a time.
 */
public final class LineOutputStream extends OutputStream {

    private static final int BUFFER_CAPACITY = 1024;

    private final Consumer<String> onLine;
    private StringBuilder builder;

    public LineOutputStream( Consumer<String> onLine ) {
        this.onLine = onLine;
        builder = new StringBuilder( BUFFER_CAPACITY );
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
        onLine.accept( builder.toString() );
        builder = new StringBuilder( BUFFER_CAPACITY );
    }

    @Override
    public void close() throws IOException {
        consumeLine();
    }

}
