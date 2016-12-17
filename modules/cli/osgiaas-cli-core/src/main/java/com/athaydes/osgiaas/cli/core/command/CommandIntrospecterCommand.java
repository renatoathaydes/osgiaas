package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.cli.CommandHelper;
import com.athaydes.osgiaas.cli.CommandInvocation;
import com.athaydes.osgiaas.cli.args.ArgsSpec;
import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Command-introspecter command.
 */
public class CommandIntrospecterCommand implements Command {

    public static String VERBOSE_ARG = "-v";
    public static String VERBOSE_LONG_ARG = "--verbose";

    private final Map<String, CommandEntry> serviceReferenceByCommand = new ConcurrentHashMap<>();

    private final ArgsSpec argsSpec = ArgsSpec.builder()
            .accepts( VERBOSE_ARG, VERBOSE_LONG_ARG )
            .withDescription( "show verbose output" )
            .end()
            .build();

    private final Comparator<Map.Entry<String, CommandEntry>> comparator =
            Comparator.comparing( Map.Entry::getKey );

    public void addCommand( ServiceReference<Command> commandServiceReference, Command command ) {
        serviceReferenceByCommand.put( command.getName(), new CommandEntry( commandServiceReference, command ) );
    }

    public void removeCommand( Command command ) {
        serviceReferenceByCommand.remove( command.getName() );
    }

    @Override
    public String getName() {
        return "ci";
    }

    @Override
    public String getUsage() {
        return "ci " + argsSpec.getUsage() + " <command-pattern>";
    }

    @Override
    public String getShortDescription() {
        return "Introspects CLI commands.\n\n" +
                "For example, to introspect (ie. see all information about) the 'ps' command, type:\n\n" +
                ">> ci ps\n\n" +
                "The ci command supports the following options:\n\n" +
                argsSpec.getDocumentation( "  " ) +
                "\n\n" +
                "The <command-pattern> argument is a regular expression, so to see information about all " +
                "commands starting with 'g', for example, you can type:\n\n" +
                ">> ci -v g.*\n";
    }

    @Override
    public void execute( String line, PrintStream out, PrintStream err ) {
        CommandInvocation invocation = argsSpec.parse( line );
        List<String> arguments = CommandHelper.breakupArguments( invocation.getUnprocessedInput(), 2 );

        if ( arguments.size() > 1 ) {
            CommandHelper.printError( err, getUsage(), "Too many arguments" );
        } else {
            Predicate<? super Map.Entry<String, CommandEntry>> matchingArguments =
                    arguments.isEmpty() ? entry -> true :
                            entry -> entry.getKey().matches( arguments.get( 0 ) );

            serviceReferenceByCommand.entrySet().stream()
                    .sorted( comparator )
                    .filter( matchingArguments )
                    .map( Map.Entry::getValue )
                    .forEach( entry -> out.println( summaryOf( entry, invocation.hasOption( VERBOSE_ARG ) ) ) );
        }
    }

    private String summaryOf( CommandEntry commandEntry, boolean verbose ) {
        Bundle bundle = commandEntry.commandServiceReference.getBundle();
        Map<String, String> serviceProperties = readServiceProperties( commandEntry.commandServiceReference );

        String bundleId = "[" + bundle.getBundleId() + "]";
        Command command = commandEntry.command;
        String bundleDescription = bundle.getSymbolicName() == null
                ? bundleId
                : bundle.getSymbolicName() + " " + bundleId;

        return "Command     : " + command.getName() +
                "\nUsage       : " + command.getUsage() +
                ( verbose ? "\nDescription : " + command.getShortDescription() : "" ) +
                ( verbose ? "\nBundle      : " + bundleDescription : "" ) +
                ( verbose ? "\nClass name  : " + command.getClass().getName() : "" ) +
                ( verbose ? "\nProperties  : " + serviceProperties : "" ) +
                "\n---";
    }

    private static Map<String, String> readServiceProperties( ServiceReference<Command> commandServiceReference ) {
        Map<String, String> result = new HashMap<>( commandServiceReference.getPropertyKeys().length );

        for (String key : commandServiceReference.getPropertyKeys()) {
            @Nullable Object value = commandServiceReference.getProperty( key );
            if ( value instanceof Object[] ) {
                value = Arrays.deepToString( ( Object[] ) value );
            }
            String stringValue = value == null ? "<null>" : value.toString();
            result.put( key, stringValue );
        }

        return result;
    }

    private static class CommandEntry {
        final ServiceReference<Command> commandServiceReference;
        final Command command;

        public CommandEntry( ServiceReference<Command> commandServiceReference, Command command ) {
            this.commandServiceReference = commandServiceReference;
            this.command = command;
        }
    }
}
