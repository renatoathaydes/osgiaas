package com.athaydes.osgiaas.api.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * An OutputStream which will accumulate the whole output it receives, sending it out all at once
 * to the provided consumer when this stream is closed.
 */
public class LineAccumulatorOutputStream extends OutputStream {

    private final LineOutputStream delegate;
    private final LinkedList<String> lines;
    private final Consumer<String> fullOutputConsumer;

    public LineAccumulatorOutputStream( Consumer<String> fullOutputConsumer,
                                        AutoCloseable closeWhenDone ) {
        this.fullOutputConsumer = fullOutputConsumer;
        lines = new LinkedList<>();
        delegate = new LineOutputStream( lines::add, closeWhenDone );
    }

    @Override
    public void write( int b ) throws IOException {
        delegate.write( b );
    }

    @Override
    public void close() throws IOException {
        fullOutputConsumer.accept( String.join( "\n", lines ) );
        delegate.close();
    }

}
