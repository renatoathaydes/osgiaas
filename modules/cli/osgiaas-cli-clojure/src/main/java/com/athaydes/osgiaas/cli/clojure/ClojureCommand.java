package com.athaydes.osgiaas.cli.clojure;

import clojure.lang.Compiler;
import clojure.lang.IFn;
import clojure.lang.Namespace;
import clojure.lang.Symbol;
import clojure.lang.Var;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.StreamingCommand;
import org.osgi.service.component.ComponentContext;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ClojureCommand implements StreamingCommand {

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();

    private final Namespace userNamespace = Namespace.findOrCreate( Symbol.create( "user" ) );
    private final Symbol outSym = Symbol.create( "out" );
    private final Symbol errSym = Symbol.create( "err" );
    private final Symbol ctxSym = Symbol.create( "ctx" );

    public void activate( ComponentContext context ) throws Exception {
        contextRef.set( context );
    }

    public void deactivate( ComponentContext context ) throws Exception {
        contextRef.set( null );
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
        return "Executes Clojure code as a REPL.\n\n" +
                "Simple Example:\n" +
                ">> clj (+ 2 2)\n" +
                "< 4\n\n" +
                "The following variables are always available in the \"user\" namespace:\n\n" +
                "  * out - the command output stream.\n" +
                "  * err - the command error stream.\n" +
                "  * ctx - the Clojure command's OSGi service ComponentContext.\n\n" +
                "To access REPL functions, you can require it like this:\n\n" +
                ">> clj (require '[clojure.repl :as repl])\n\n" +
                "You can then access docs, for example:\n\n" +
                ">> clj (repl/doc +)\n\n" +
                "When run through pipes, the Clojure code should return a fn that takes each line as input.\n\n" +
                "For example:\n\n" +
                ">> some_command | clj (fn [line] (str \"- \" line))";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        @Nullable Object result = runCommand( line, out, err );

        if ( result != null ) {
            out.println( result );
        }
    }

    @Nullable
    private Object runCommand( String line, PrintStream out, PrintStream err ) {
        List<String> parts = CommandHelper.breakupArguments( line, 2 );

        if ( parts.size() == 2 ) {
            String cljCode = parts.get( 1 );
            Var.intern( userNamespace, outSym, out );
            Var.intern( userNamespace, errSym, err );
            Var.intern( userNamespace, ctxSym, contextRef.get() );

            try {
                return Compiler.load( new StringReader( cljCode ) );
            } catch ( Exception e ) {
                err.println( e );
            }
        }

        return null;
    }

    @Override
    public Consumer<String> pipe( String command, PrintStream out, PrintStream err ) {
        @Nullable Object fn = runCommand( command, out, err );

        if ( fn instanceof IFn ) {
            return ( line ) -> {
                @Nullable Object result = ( ( IFn ) fn ).invoke( line );
                if ( result != null ) {
                    out.println( result );
                }
            };
        }

        throw new RuntimeException( "When used in a pipeline, the Clojure script must return a " +
                "lambda that takes one text line of the input at a time.\n" +
                "Example:\nsome-command | clj (fn [line] (str \"- \" line))" );
    }
}
