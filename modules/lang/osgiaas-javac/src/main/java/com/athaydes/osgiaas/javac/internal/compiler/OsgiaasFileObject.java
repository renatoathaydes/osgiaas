package com.athaydes.osgiaas.javac.internal.compiler;

import com.athaydes.osgiaas.javac.internal.CompilerUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.tools.SimpleJavaFileObject;

final class OsgiaasFileObject extends SimpleJavaFileObject {

    private final ByteArrayOutputStream contents = new ByteArrayOutputStream( 256 );

    OsgiaasFileObject( String simpleClassName, CharSequence source ) {
        super( URI.create( simpleClassName + CompilerUtils.JAVA_EXTENSION ),
                Kind.SOURCE );
        try {
            contents.write( source.toString().getBytes( StandardCharsets.UTF_8 ) );
        } catch ( IOException e ) {
            throw new RuntimeException( e ); // should never happen - no real IO happening
        }
    }

    OsgiaasFileObject( String qualifiedClassName, Kind kind ) {
        super( URI.create( qualifiedClassName ), kind );
    }

    @Override
    public CharSequence getCharContent( boolean ignoreEncodingErrors ) {
        return new String( contents.toByteArray(), StandardCharsets.UTF_8 );
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream( contents.toByteArray() );
    }

    @Override
    public OutputStream openOutputStream() {
        return contents;
    }

    byte[] getByteCode() {
        if ( getKind() == Kind.CLASS ) {
            return contents.toByteArray();
        }
        throw new UnsupportedOperationException( "FileObject is not of kind CLASS - cannot provide bytecode" );
    }

    @Override
    public String toString() {
        return "OsgiaasFileObject{" +
                "name=" + getName() +
                ", kind=" + getKind() +
                '}';
    }
}
