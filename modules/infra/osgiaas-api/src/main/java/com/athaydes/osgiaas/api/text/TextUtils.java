package com.athaydes.osgiaas.api.text;

/**
 * Utility functions to process text.
 */
public final class TextUtils {

    private TextUtils() {
        // static functions only
    }

    /**
     * Pads the given text to the right.
     *
     * @param text    to pad
     * @param padding minimum length of the resulting text, padding to the right if necessary with whitespaces.
     * @return padded text if necessary, or the provided text if no padding is needed.
     */
    public static String padRight( String text, int padding ) {
        return String.format( "%1$-" + padding + "s", text );
    }

    /**
     * Pads the given text to the left.
     *
     * @param text    to pad
     * @param padding minimum length of the resulting text, padding to the left if necessary with whitespaces.
     * @return padded text if necessary, or the provided text if no padding is needed.
     */
    public static String padLeft( String text, int padding ) {
        return String.format( "%1$" + padding + "s", text );
    }

}
