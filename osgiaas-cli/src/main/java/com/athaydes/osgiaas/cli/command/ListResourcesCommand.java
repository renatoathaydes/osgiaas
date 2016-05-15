package com.athaydes.osgiaas.cli.command;

import com.athaydes.osgiaas.api.cli.CommandInvocation;
import com.athaydes.osgiaas.api.cli.args.ArgsSpec;
import com.athaydes.osgiaas.api.service.DynamicServiceHelper;
import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

public class ListResourcesCommand implements Command {

    public static final String RECURSIVE_OPTION = "-r";
    public static final String PATTERN_OPTION = "-p";

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();

    public static final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( RECURSIVE_OPTION )
            .accepts( PATTERN_OPTION, false, true )
            .build();

    public static Function<String, String> searchTransform = ( search ) ->
            "/" + search.replaceAll( "\\.", "/" );

    public void activate( ComponentContext context ) {
        contextRef.set( context );
    }

    public void deactivate( ComponentContext context ) {
        contextRef.set( null );
    }

    @Override
    public String getName() {
        return "lr";
    }

    @Override
    public String getUsage() {
        return "lr <package-name>";
    }

    @Override
    public String getShortDescription() {
        return "List resources";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        DynamicServiceHelper.with( contextRef, context -> {
            CommandInvocation invocation = argsSpec.parse( line );

            listResources( invocation, context, searchTransform )
                    .forEach( out::println );
        }, () -> err.println( "ComponentContext is unavailable" ) );
    }

    private static Stream<BundleWiring> wiringsOf( Stream<Bundle> bundles ) {
        return bundles.map( b -> b.adapt( BundleWiring.class ) );
    }

    public static Stream<String> listResources( CommandInvocation invocation,
                                                ComponentContext componentContext,
                                                Function<String, String> searchTransform ) {

        int lrOption = invocation.hasArg( RECURSIVE_OPTION ) ?
                BundleWiring.LISTRESOURCES_RECURSE :
                BundleWiring.LISTRESOURCES_LOCAL;

        String pattern = invocation.hasArg( PATTERN_OPTION ) ?
                invocation.getArgValue( PATTERN_OPTION ) :
                "*";

        String searchWord = searchTransform.apply( invocation.getUnprocessedInput() );

        return wiringsOf( Stream.of( componentContext.getBundleContext().getBundles() ) )
                .filter( wiring -> wiring != null )
                .flatMap( wiring -> wiring.listResources( searchWord, pattern, lrOption ).stream() )
                .distinct();
    }

}
