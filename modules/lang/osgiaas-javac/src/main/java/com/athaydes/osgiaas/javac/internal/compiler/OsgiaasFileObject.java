package com.athaydes.osgiaas.javac.internal.compiler;

import com.athaydes.osgiaas.javac.internal.CompilerUtils;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

final class OsgiaasFileObject extends SimpleJavaFileObject {

    // If kind == CLASS, this stores byte code from openOutputStream
    private ByteArrayOutputStream byteCode;

    // if kind == SOURCE, this contains the source text
    private final CharSequence source;

    OsgiaasFileObject( String simpleClassName, CharSequence source ) {
        super( URI.create( simpleClassName + CompilerUtils.JAVA_EXTENSION ),
                Kind.SOURCE );
        this.source = source;
    }

    OsgiaasFileObject( String qualifiedClassName, Kind kind ) {
        super( URI.create( qualifiedClassName ), kind );
        source = null;
    }

    @Override
    public CharSequence getCharContent( boolean ignoreEncodingErrors )
            throws IOException {
        if ( source == null ) {
            throw new UnsupportedOperationException( "getCharContent() not supported for this resource" );
        }
        return source;
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream( getByteCode() );
    }

    @Override
    public OutputStream openOutputStream() {
        byteCode = new ByteArrayOutputStream();
        return byteCode;
    }

    byte[] getByteCode() {
        return byteCode.toByteArray();
    }
}
