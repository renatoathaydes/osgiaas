package com.athaydes.osgiaas.cli.java;

import org.osgi.framework.BundleContext;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Binding for Java scripts
 */
@SuppressWarnings( "WeakerAccess" )
public class Binding {

    public static PrintStream out;
    public static PrintStream err;
    public static BundleContext ctx;
    public static final Map<Object, Object> binding = new HashMap<>();

}
