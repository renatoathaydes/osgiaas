package com.athaydes.osgiaas.cli.util;

import com.athaydes.osgiaas.api.ansi.AnsiColor;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps the actual outputStream to allow customization of what happens to text sent by
 * Commands.
 */
public class OsgiaasPrintStream extends PrintStream {

    private final AnsiColor color;
    private final AtomicBoolean hasWritten = new AtomicBoolean( false );

    public OsgiaasPrintStream( PrintStream out, AnsiColor color ) {
        super( out );
        this.color = color;
    }

    @Override
    public void write( int b ) {
        if ( !hasWritten.getAndSet( true ) ) {
            byte[] colorBytes = color.toString().getBytes();
            super.write( colorBytes, 0, colorBytes.length );
        }
        super.write( b );

    }

    @Override
    public void write( byte[] buf, int off, int len ) {
        if ( !hasWritten.getAndSet( true ) ) {
            byte[] colorBytes = color.toString().getBytes();
            super.write( colorBytes, 0, colorBytes.length );
        }
        super.write( buf, off, len );
    }

}
