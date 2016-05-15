package com.athaydes.osgiaas.js.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import org.apache.felix.shell.Command;
import org.osgi.service.component.ComponentContext;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JSCommand implements Command {

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();

    @Nullable
    private volatile ScriptEngine engine;

    public JSCommand() {
        this.engine = getEngine();
    }

    public void activate( ComponentContext context ) throws Exception {
        contextRef.set( context );
    }

    public void deactivate( ComponentContext context ) throws Exception {
        contextRef.set( null );
    }

    @Nullable
    private ScriptEngine getEngine() {
        @Nullable ScriptEngine result = engine;
        if ( engine == null ) {
            result = new ScriptEngineManager().getEngineByName( "JavaScript" );
            engine = result;
        }
        return result;
    }

    @Override
    public String getName() {
        return "js";
    }

    @Override
    public String getUsage() {
        return "js <javascript script>";
    }

    @Override
    public String getShortDescription() {
        return "The js command runs JavaScript (Nashorn) scripts.";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        List<String> parts = CommandHelper.breakupArguments( line, 2 );

        if ( parts.size() == 2 ) {
            String script = parts.get( 1 );

            @Nullable ScriptEngine engine = getEngine();

            if ( engine == null ) {
                err.println( "JavaScript engine is not available" );
            } else try {
                Bindings bindings = engine.getBindings( ScriptContext.GLOBAL_SCOPE );
                bindings.put( "out", out );
                bindings.put( "err", err );
                bindings.put( "ctx", contextRef.get() );

                @Nullable Object result = engine.eval( script );

                if ( result != null ) {
                    out.println( result );
                }
            } catch ( ScriptException e ) {
                err.println( e.toString() );
            }
        }
    }
}
