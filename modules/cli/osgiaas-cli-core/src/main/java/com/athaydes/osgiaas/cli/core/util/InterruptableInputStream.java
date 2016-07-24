package com.athaydes.osgiaas.cli.core.util;

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
