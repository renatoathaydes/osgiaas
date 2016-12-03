package com.athaydes.osgiaas.cli.core.command;

import com.athaydes.osgiaas.cli.CommandModifier;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartCommandModifier implements CommandModifier {

    private static final Pattern START_OSGIAAS_COMMAND_PATTERN = Pattern.compile( "\\s*(start|install)\\s+([a-z]+)\\s*" );

    @Override
    public List<String> apply( String command ) {
        Matcher matcher = START_OSGIAAS_COMMAND_PATTERN.matcher( command );

        if ( matcher.matches() ) {
            String dependency = matcher.group( 2 );
            String expandedDependency = expand( dependency );
            if ( expandedDependency != null ) {
                System.out.println( "Expanding dependency from " + dependency + " to " + expandedDependency );
                command = matcher.group( 1 ) + " " + expandedDependency;
            }
        }

        return Collections.singletonList( command );
    }

    @Nullable
    private String expand( String dependency ) {
        switch ( dependency ) {
            case "js":
                return "mvn:com.athaydes.osgiaas/osgiaas-cli-js";
            case "java":
                return "mvn:com.athaydes.osgiaas/osgiaas-cli-java";
            case "frege":
                return "mvn:com.athaydes.osgiaas/osgiaas-cli-frege";
            case "groovy":
                return "mvn:com.athaydes.osgiaas/osgiaas-cli-groovy";
            default:
                return null;
        }
    }
}
