package com.athaydes.osgiaas.grab

/**
 * Exception thrown when it is not possible to grab an artifact due to some error.
 */
class GrabException extends RuntimeException {

    GrabException( String message ) {
        super( message )
    }

}
