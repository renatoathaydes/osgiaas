package com.athaydes.osgiaas.cli.util;

import java.io.IOException;
import java.io.InputStream;

public class InterruptableInputStream extends InputStream {

    private final InputStream in;

    public InterruptableInputStream( InputStream in ) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        while ( !Thread.interrupted() ) {
            if ( in.available() > 0 ) {
                return in.read();
            }
            try {
                Thread.sleep( 25L );
            } catch ( InterruptedException e ) {
                break;
            }
        }

        // got interrupted, return -1 to report EOI
        return -1;
    }

}
