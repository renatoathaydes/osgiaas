package com.athaydes.osgiaas.cli.util;

import com.athaydes.osgiaas.api.cli.AnsiColor;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wraps the actual outputStream to allow customization of what happens to text sent by
 * Commands.
 */
public class OsgiaasPrintStream extends PrintStream {

    private final AtomicReference<AnsiColor> color = new AtomicReference<>();
    private final AtomicBoolean hasWritten = new AtomicBoolean( false );

    public OsgiaasPrintStream( PrintStream out ) {
        super( out );
    }

    @Override
    public void write( int b ) {
        if ( !hasWritten.getAndSet( true ) ) {
            DynamicServiceHelper.with( color, ( currentColor ) -> {
                byte[] colorBytes = currentColor.toString().getBytes();
                super.write( colorBytes, 0, colorBytes.length );
            } );
        }
        super.write( b );

    }

    @Override
    public void write( byte[] buf, int off, int len ) {
        if ( !hasWritten.getAndSet( true ) ) {
            DynamicServiceHelper.with( color, ( currentColor ) -> {
                byte[] colorBytes = currentColor.toString().getBytes();
                super.write( colorBytes, 0, colorBytes.length );
            } );
        }
        super.write( buf, off, len );
    }

    public void setColor( @Nullable AnsiColor color ) {
        this.color.set( color );
    }

}
