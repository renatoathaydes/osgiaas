package com.athaydes.osgiaas.cli.scala;

import com.athaydes.osgiaas.api.stream.LineOutputStream;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.StreamingCommand;
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

public class ScalaCommand implements StreamingCommand {

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();

    @Nullable
    private volatile ScriptEngine engine;

    public ScalaCommand() {
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
            result = new ScriptEngineManager()
                    .getEngineByName( "scala" );
            engine = result;
        }
        return result;
    }

    @Override
    public String getName() {
        return "scala";
    }

    @Override
    public String getUsage() {
        return "scala <scala script>";
    }

    @Override
    public String getShortDescription() {
        return "The Scala command runs Scala scripts.\n\n" +
                "For example:\n\n" +
                ">> scala 2 + 2\n" +
                "< 4\n\n" +
                "When run through pipes, the Scala code should return a function that takes " +
                "each input line as an argument, returning something to be printed (or null).\n\n" +
                "For example, to only print the lines containing the word 'text' from the output of some_command:\n\n" +
                ">> some_command | scala (line: String) => if (line.contains(\"text\")) line else null";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        List<String> parts = CommandHelper.breakupArguments( line, 2 );

        if ( parts.size() == 2 ) {
            String script = parts.get( 1 );

            @Nullable ScriptEngine engine = getEngine();

            if ( engine == null ) {
                err.println( "Scala engine is not available" );
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
    public OutputStream pipe( String line, PrintStream out, PrintStream err ) {
        @Nullable ScriptEngine engine = getEngine();

        if ( engine == null ) {
            throw new RuntimeException( "Scala engine is not available." );
        }

        String command = line.substring( "scala".length() );

        try {
            Object result = runScript( out, err, engine, command );
            if ( result instanceof scala.Function1 ) {
                return new LineOutputStream( l ->
                {
                    //noinspection unchecked
                    @Nullable Object returnedValue = ( ( scala.Function1 ) result ).apply( l );

                    if ( returnedValue != null ) {
                        out.println( returnedValue );
                    }
                }, out );
            } else {
                err.println( "Scala returned instance of " + ( result == null ? "null" : result.getClass() ) );
            }

            throw new RuntimeException( "When used in a pipeline, the Scala script must return a " +
                    "function callback to run for each input line." );
        } catch ( ScriptException e ) {
            throw new RuntimeException( e );
        }
    }

    @Nullable
    private Object runScript( PrintStream out, PrintStream err,
                              ScriptEngine engine, String script )
            throws ScriptException {
        String printStreamClassName = PrintStream.class.getName();

        // FIXME ctx cannot be added to the bindings because its class is not visible from the Scala classpath
        String preamble = "import " + printStreamClassName + "\n" +
                //"val ctx = _ctx.asInstanceOf[%s]\n"+
                "val out = _out.asInstanceOf[" + printStreamClassName + "]\n" +
                "val err = _err.asInstanceOf[" + printStreamClassName + "]\n";

        Bindings bindings = engine.getBindings( ScriptContext.GLOBAL_SCOPE );

        bindings.put( "_out", out );
        bindings.put( "_err", err );
        bindings.put( "_ctx", contextRef.get() );

        return engine.eval( preamble + script );
    }

}
