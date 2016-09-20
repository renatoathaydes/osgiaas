package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.api.service.DynamicServiceHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

public class ListResourcesCommand implements Command {

    public static final String RECURSIVE_OPTION = "-r";
    public static final String SHOW_ALL_OPTION = "-a";
    public static final String PATTERN_OPTION = "-p";

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();

    public static final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( RECURSIVE_OPTION ).end()
            .accepts( SHOW_ALL_OPTION ).end()
            .accepts( PATTERN_OPTION ).withArgCount( 1 ).end()
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
                "  -r: recursively list resources under sub-paths.\n" +
                "  -a: show all resources, including nested classes.\n" +
                "  -p: pattern to search.\n" +
                "\n" +
                "For example, to list all class files available under the 'com' package:\n" +
                "\n" +
                "lr -r -p *.class com/\n";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        DynamicServiceHelper.with( contextRef, context -> {
            CommandInvocation invocation = argsSpec.parse( line );

            listResources( invocation, context, searchTransform )
                    .forEach( out::println );
        }, () -> err.println( "BundleContext is unavailable" ) );
    }

    private static Stream<BundleWiring> wiringsOf( Stream<Bundle> bundles ) {
        return bundles.map( b -> b.adapt( BundleWiring.class ) );
    }

    public static Stream<String> listResources( CommandInvocation invocation,
                                                BundleContext bundleContext,
                                                Function<String, String> searchTransform ) {

        int lrOption = invocation.hasArg( RECURSIVE_OPTION ) ?
                BundleWiring.LISTRESOURCES_RECURSE :
                BundleWiring.LISTRESOURCES_LOCAL;

        boolean showAll = invocation.hasArg( SHOW_ALL_OPTION );

        String pattern = invocation.hasArg( PATTERN_OPTION ) ?
                invocation.getArgValue( PATTERN_OPTION ) :
                "*";

        String searchWord = searchTransform.apply( invocation.getUnprocessedInput() );

        return wiringsOf( Stream.of( bundleContext.getBundles() ) )
                .filter( wiring -> wiring != null )
                .flatMap( wiring -> wiring.listResources( searchWord, pattern, lrOption ).stream() )
                .filter( resource -> showAll || !resource.contains( "$" ) )
                .distinct();
    }

}
