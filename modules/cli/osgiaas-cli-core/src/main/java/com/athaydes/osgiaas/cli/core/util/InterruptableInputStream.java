package com.athaydes.osgiaas.cli.core.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public class InterruptableInputStream extends InputStream {

    private final InputStream in;
    private volatile boolean alive = true;

    private long idleCycles = 1L;

    public InterruptableInputStream( InputStream in ) {
        this.in = in;
    }

    @Override
    public int read( @Nonnull byte[] b ) throws IOException {
        return in.read( b );
    }

    @Override
    public int read( @Nonnull byte[] b, int off, int len ) throws IOException {
        return in.read( b, off, len );
    }

    @Override
    public long skip( long n ) throws IOException {
        return in.skip( n );
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void mark( int readlimit ) {
        in.mark( readlimit );
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws IOException {
        long period = 25L;
        long minCyclesToLongWait = 40L
                * 120L;// seconds
        long longWaitCycles = 10L;

        while ( alive ) {
            if ( in.available() > 0 ) {
                idleCycles = 0;
                return in.read();
            }
            idleCycles++;
            try {
                long sleepCycles = ( idleCycles > minCyclesToLongWait ? longWaitCycles : 1L );
                Thread.sleep( period * sleepCycles );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
                break;
            }
        }

        // got interrupted, return -1 to report EOI
        return -1;
    }

    public void interrupt() {
        alive = false;
    }

}
