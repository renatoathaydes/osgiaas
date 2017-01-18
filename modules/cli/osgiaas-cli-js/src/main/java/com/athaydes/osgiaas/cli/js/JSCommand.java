package com.athaydes.osgiaas.cli.js;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.StreamingCommand;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
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
import java.util.function.Consumer;

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
        return "The js command runs JavaScript (Nashorn) scripts.\n\n" +
                "For example:\n\n" +
                ">> js 2 + 2\n" +
                "< 4\n\n" +
                "When run through pipes, the JS code should return a function that takes " +
                "each input line as an argument, returning something to be printed (or null).\n\n" +
                "For example, to only print the lines containing the word 'text' from the output of some_command:\n\n" +
                ">> some_command | js function(line) { if (line.contains(\"text\")) line; }";
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
                @Nullable Object result = runScript( out, err, engine, script );
                if ( result != null ) {
                    out.println( result );
                }
            } catch ( ScriptException e ) {
                err.println( e.toString() );
            }
        }
    }

    @Override
    public Consumer<String> pipe( String line, PrintStream out, PrintStream err ) {
        @Nullable ScriptEngine engine = getEngine();

        if ( engine == null ) {
            throw new RuntimeException( "JavaScript engine is not available." );
        }

        String command = line.substring( "js".length() );

        try {
            Object result = runScript( out, err, engine, command );
            if ( result instanceof ScriptObjectMirror ) {
                ScriptObjectMirror mirror = ( ScriptObjectMirror ) result;
                if ( mirror.isFunction() ) {

                    //noinspection CollectionAddedToSelf
                    return l -> {
                        @Nullable Object returnedValue = mirror.call( result, l );

                        if ( returnedValue != null && !ScriptObjectMirror.isUndefined( returnedValue ) ) {
                            out.println( returnedValue );
                        }
                    };
                }
            }

            throw new RuntimeException( "When used in a pipeline, the JS script must return a " +
                    "function callback to run for each input line." );
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

        return engine.eval( script );
    }

}
