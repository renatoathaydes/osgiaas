package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.api.service.DynamicServiceHelper;
import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InspectCommandCompleter implements CommandCompleter {

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();
    private final Completer completer;

    public InspectCommandCompleter() {
        Supplier<Stream<CompletionMatcher>> inspectArgChildren = () -> Stream.of(
                CompletionMatcher.nameMatcher( "capability", this::bundleIdMatchers ),
                CompletionMatcher.nameMatcher( "requirement", this::bundleIdMatchers ) );

        this.completer = new Completer( inspectArgChildren );
    }

    public void activate( BundleContext context ) {
        contextRef.set( context );
    }

    public void deactivate( BundleContext context ) {
        contextRef.set( null );
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        return completer.complete( buffer, cursor, candidates );
    }

    private Stream<CompletionMatcher> bundleIdMatchers() {
        return DynamicServiceHelper.let( contextRef, context ->
                        Stream.of( context.getBundles() )
                                .map( Bundle::getBundleId )
                                .map( id -> CompletionMatcher.nameMatcher( Long.toString( id ) ) ),
                Stream::of );
    }

    private static class Completer extends BaseCompleter {

        Completer( Supplier<Stream<CompletionMatcher>> inspectArgChildren ) {
            super( CompletionMatcher.nameMatcher( "inspect",
                    CompletionMatcher.nameMatcher( "bundle", inspectArgChildren ),
                    CompletionMatcher.nameMatcher( "package", inspectArgChildren ),
                    CompletionMatcher.nameMatcher( "fragment", inspectArgChildren ),
                    CompletionMatcher.nameMatcher( "service", inspectArgChildren ) ) );
        }

    }

}
