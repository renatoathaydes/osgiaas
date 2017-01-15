package com.athaydes.osgiaas.api.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * OutputStream that can be used to receive a line of text at a time from a writer.
 */
public final class LineOutputStream extends OutputStream {

    private static final int DEFAULT_BUFFER_CAPACITY = 1024;

    private final Consumer<String> onLine;
    private final LinkedList<ByteBuffer> buffers = new LinkedList<>();
    private final AutoCloseable closeWhenDone;
    private final int bufferCapacity;

    private ByteBuffer buffer;

    /**
     * Create an instance of {@link LineOutputStream} using the default buffer capacity.
     *
     * @param onLine        callback to run on each line of text received.
     * @param closeWhenDone callback to run when this stream gets closed.
     */
    public LineOutputStream( Consumer<String> onLine, AutoCloseable closeWhenDone ) {
        this( onLine, closeWhenDone, DEFAULT_BUFFER_CAPACITY );
    }

    /**
     * Create an instance of {@link LineOutputStream}.
     *
     * @param onLine         callback to run on each line of text received.
     * @param closeWhenDone  callback to run when this stream gets closed.
     * @param bufferCapacity internal buffer capacity.
     *                       If the given capacity is not enough, new buffers are created as needed.
     */
    public LineOutputStream( Consumer<String> onLine,
                             AutoCloseable closeWhenDone,
                             int bufferCapacity ) {
        this.onLine = onLine;
        this.closeWhenDone = closeWhenDone;
        this.bufferCapacity = bufferCapacity;

        if ( bufferCapacity < 1 ) {
            throw new IllegalArgumentException( "bufferCapacity must be greater than 0" );
        }

        startNewByteBuffer();
    }

    private void startNewByteBuffer() {
        buffer = ByteBuffer.allocate( bufferCapacity );
        buffers.add( buffer );
    }

    private String readBuffers() {
        int totalSize = 0;
        for (int i = 0; i < buffers.size() - 1; i++) {
            totalSize += bufferCapacity;
        }

        totalSize += buffers.getLast().position();

        byte[] finalArray = new byte[ totalSize ];

        // unload all full ByteBuffers into the final array
        int currentFirstIndex = 0;
        while ( buffers.size() > 1 ) {
            ByteBuffer currentBuffer = buffers.removeFirst();
            currentBuffer.rewind();
            currentBuffer.get( finalArray, currentFirstIndex, bufferCapacity );
            currentFirstIndex += bufferCapacity;
        }

        // unload the last ByteBuffer now (do not remove it as there must be always one buffer at least)
        buffer = buffers.getFirst();
        int bufferPosition = buffer.position();
        buffer.rewind();
        buffer.get( finalArray, currentFirstIndex, bufferPosition );
        buffer.rewind();

        return new String( finalArray, StandardCharsets.UTF_8 );
    }

    @Override
    public void write( int b ) throws IOException {
        if ( b == '\n' ) {
            consumeLine();
            return;
        }

        if ( buffer.position() >= bufferCapacity ) {
            startNewByteBuffer();
        }

        // as specified by the write() docs, we just throw away the 24 high-order bits
        buffer.put( ( byte ) b );
    }

    private void consumeLine() {
        if ( buffers.size() > 1 || buffer.position() > 0 ) {
            onLine.accept( readBuffers() );
        }
    }

    @Override
    public void close() throws IOException {
        consumeLine();
        buffers.clear();
        try {
            closeWhenDone.close();
        } catch ( Exception e ) {
            throw new IOException( e );
        }
    }

}
