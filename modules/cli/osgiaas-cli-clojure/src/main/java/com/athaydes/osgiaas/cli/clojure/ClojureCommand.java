package com.athaydes.osgiaas.cli.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.athaydes.osgiaas.cli.CommandHelper;
import org.apache.felix.shell.Command;
import org.osgi.service.component.ComponentContext;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ClojureCommand implements Command {

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();
    private final AtomicReference<IFn> evalRef = new AtomicReference<>();

    public void activate( ComponentContext context ) throws Exception {
        contextRef.set( context );

        CompletableFuture.runAsync( () -> evalRef.set( Clojure.var( "clojure.core", "eval" ) ) );
    }

    public void deactivate( ComponentContext context ) throws Exception {
        contextRef.set( null );
        evalRef.set( null );
    }

    @Override
    public String getName() {
        return "clj";
    }

    @Override
    public String getUsage() {
        return "clj <Clojure code>";
    }

    @Override
    public String getShortDescription() {
        return "Clojure REPL";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        @Nullable IFn eval = evalRef.get();

        if ( eval == null ) {
            err.println( "Clojure engine is not ready. Try again soon." );
            return;
        }

        List<String> parts = CommandHelper.breakupArguments( line, 2 );

        if ( parts.size() == 2 ) {
            String cljCode = parts.get( 1 );

            try {
                Object result = Clojure.read( cljCode );

                if ( result != null ) {
                    out.println( eval.invoke( result ) );
                }
            } catch ( Exception e ) {
                err.println( e );
            }
        }
    }

}
