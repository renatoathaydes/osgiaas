package com.athaydes.osgiaas.cli.frege;

import org.apache.felix.shell.Command;
import org.osgi.framework.BundleContext;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.PrintStream;

public class FregeCommand implements Command {

    private final ScriptEngine scriptEngine;
    private BundleContext bundleContext;

    public FregeCommand() {
        scriptEngine = new ScriptEngineManager().getEngineByName( "frege" );
    }

    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getName() {
        return "frege";
    }

    @Override
    public String getUsage() {
        return "frege <script>";
    }

    @Override
    public String getShortDescription() {
        return "Executes a Frege script";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        if ( scriptEngine == null ) {
            err.println( "Frege ScriptEngine not found!\n" +
                    "Make sure to make the Frege jar available in the classpath before trying to use it.\n" +
                    "If you use the osgi-run Gradle plugin, add frege-interpreter-core as a systemLib dependency." );
        } else {
            Bindings bindings = scriptEngine.getBindings( ScriptContext.ENGINE_SCOPE );

            bindings.put( "out", out );
            bindings.put( "err", err );
            bindings.put( "ctx", bundleContext );

            try {
                @Nullable Object result = scriptEngine.eval( line.substring( getName().length() + 1 ) );
                if ( result != null ) {
                    out.println( result );
                }
            } catch ( ScriptException e ) {
                err.println( e.toString() );
            }
        }

    }
}
