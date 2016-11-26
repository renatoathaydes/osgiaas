package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.api.service.DynamicServiceHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.api.text.TextUtils.padLeft;
import static com.athaydes.osgiaas.api.text.TextUtils.padRight;

public class ListResourcesCommand implements Command {

    public static final String RECURSIVE_OPTION = "-r";
    public static final String RECURSIVE_LONG_OPTION = "--recurse";
    public static final String SHOW_ALL_OPTION = "-a";
    public static final String SHOW_ALL_LONG_OPTION = "--all";
    public static final String PATTERN_OPTION = "-p";
    public static final String PATTERN_LONG_OPTION = "--pattern";
    public static final String VERBOSE_OPTION = "-v";
    public static final String VERBOSE_LONG_OPTION = "--verbose";

    public static final int SYM_NAME_COL_WIDTH = 30;

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();

    public static final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( RECURSIVE_OPTION, RECURSIVE_LONG_OPTION ).end()
            .accepts( SHOW_ALL_OPTION, SHOW_ALL_LONG_OPTION ).end()
            .accepts( VERBOSE_OPTION, VERBOSE_LONG_OPTION ).end()
            .accepts( PATTERN_OPTION, PATTERN_LONG_OPTION ).withArgCount( 1 ).end()
            .build();

    public static Function<String, String> searchTransform = ( search ) ->
            "/" + search.replaceAll( "\\.", "/" );

    public void activate( BundleContext context ) {
        contextRef.set( context );
    }

    public void deactivate( BundleContext context ) {
        contextRef.set( null );
    }

    @Override
    public String getName() {
        return "lr";
    }

    @Override
    public String getUsage() {
        return "lr [-r] [-p <pattern>] <package-name>";
    }

    @Override
    public String getShortDescription() {
        return "List JVM resources included in the installed bundles.\n" +
                "\n" +
                "The lr command supports the following options:\n" +
                "\n" +
                "  * " + RECURSIVE_OPTION + ", " + RECURSIVE_LONG_OPTION + ":\n" +
                "    recursively list resources under sub-paths.\n" +
                "  * " + SHOW_ALL_OPTION + ", " + SHOW_ALL_LONG_OPTION + ":\n" +
                "    show all resources, including nested classes.\n" +
                "  * " + PATTERN_OPTION + " pattern, " + PATTERN_LONG_OPTION + " pattern:\n" +
                "    file pattern to search.\n" +
                "  * " + VERBOSE_OPTION + ", " + VERBOSE_LONG_OPTION + ":\n" +
                "    show verbose output, including bundle information for each resource.\n" +
                "\n" +
                "For example, to list all class files available under the 'com' package:\n" +
                "\n" +
                "lr -r -p *.class com/\n";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        DynamicServiceHelper.with( contextRef, context -> {
            CommandInvocation invocation = argsSpec.parse( line );

            listResources( invocation, context, searchTransform, out )
                    .forEach( out::println );
        }, () -> err.println( "BundleContext is unavailable" ) );
    }

    private static Stream<BundleWiring> wiringsOf( Stream<Bundle> bundles ) {
        return bundles.map( b -> b.adapt( BundleWiring.class ) );
    }

    public static Stream<String> listResources( CommandInvocation invocation,
                                                BundleContext bundleContext,
                                                Function<String, String> searchTransform ) {
        return listResources( invocation, bundleContext, searchTransform, null );
    }

    public static Stream<String> listResources( CommandInvocation invocation,
                                                BundleContext bundleContext,
                                                Function<String, String> searchTransform,
                                                @Nullable PrintStream out ) {
        int lrOption = invocation.hasArg( RECURSIVE_OPTION ) ?
                BundleWiring.LISTRESOURCES_RECURSE :
                BundleWiring.LISTRESOURCES_LOCAL;

        boolean showAll = invocation.hasArg( SHOW_ALL_OPTION );

        String pattern = invocation.hasArg( PATTERN_OPTION ) ?
                invocation.getArgValue( PATTERN_OPTION ) :
                "*";

        String searchWord = searchTransform.apply( invocation.getUnprocessedInput() );

        boolean longForm = invocation.hasArg( VERBOSE_OPTION );

        if ( longForm && out != null ) {
            out.println( " ID   " + padRight( "Bundle Symbolic Name", SYM_NAME_COL_WIDTH ) + " Resource" );
        }

        return wiringsOf( Stream.of( bundleContext.getBundles() ) )
                .filter( Objects::nonNull )
                .flatMap( wiring -> wiring.findEntries( searchWord, pattern, lrOption )
                        .stream()
                        .filter( resource -> showAll || !resource.getPath().contains( "$" ) )
                        .map( url -> showResource( wiring, url, longForm ) ) )
                .distinct();
    }

    private static String showResource( BundleWiring wiring, URL resourceURL, boolean longForm ) {
        String resource = resourceURL.getPath();
        if ( resource.startsWith( "/" ) ) {
            resource = resource.substring( 1 );
        }

        if ( longForm ) {
            long size = -1;
            boolean isDirectory = resource.endsWith( "/" );

            if ( !isDirectory ) {
                @Nullable URLConnection connection = null;

                try {
                    connection = resourceURL.openConnection();
                    size = connection.getContentLengthLong();
                } catch ( IOException e ) {
                    e.printStackTrace();
                } finally {
                    if ( connection instanceof Closeable ) {
                        try {
                            ( ( Closeable ) connection ).close();
                        } catch ( IOException e ) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return "[" + padLeft( "" + wiring.getBundle().getBundleId(), 3 ) + "] " +
                    padRight( wiring.getBundle().getSymbolicName(), SYM_NAME_COL_WIDTH ) + " " +
                    resource +
                    ( size == -1 ? "" : " (" + size + " bytes)" );

        } else {
            return resource;
        }
    }

}
