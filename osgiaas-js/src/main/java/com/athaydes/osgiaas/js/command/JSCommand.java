package com.athaydes.osgiaas.js.command;

import com.athaydes.osgiaas.api.cli.CommandHelper;
import com.athaydes.osgiaas.api.cli.StreamingCommand;
import com.athaydes.osgiaas.api.stream.LineOutputStream;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.osgi.service.component.ComponentContext;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JSCommand implements StreamingCommand {

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
                runScript( out, err, engine, script );
            } catch ( ScriptException e ) {
                err.println( e.toString() );
            }
        }
    }

    @Override
    public OutputStream pipe( String line, PrintStream out, PrintStream err ) {
        String command = line.substring( "js ".length() );

        try {
            Object result = runScript( out, err, getEngine(), command );
            if ( result instanceof ScriptObjectMirror ) {
                ScriptObjectMirror mirror = ( ScriptObjectMirror ) result;
                if ( mirror.isFunction() ) {

                    //noinspection CollectionAddedToSelf
                    return new LineOutputStream( l ->
                            mirror.call( mirror, l ), out );
                }
            }

            throw new RuntimeException( "When used in a pipeline, the JS script must return a " +
                    "function callback that takes one text line of the input at a time.\n" +
                    "Example: ... | js function(line) { out.println(\"Line: \" + line); }" );
        } catch ( ScriptException e ) {
            throw new RuntimeException( e );
        }
    }

    @Nullable
    private Object runScript( PrintStream out, PrintStream err,
                              ScriptEngine engine, String script )
            throws ScriptException {
        Bindings bindings = engine.getBindings( ScriptContext.GLOBAL_SCOPE );
        bindings.put( "out", out );
        bindings.put( "err", err );
        bindings.put( "ctx", contextRef.get() );

        @Nullable Object result = engine.eval( script );

        if ( result != null ) {
            out.println( result );
        }

        return result;
    }

}
