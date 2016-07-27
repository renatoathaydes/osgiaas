package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;
import com.athaydes.osgiaas.cli.core.command.RunCommand;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.alternativeMatchers;
import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.anyLevelMatcher;
import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.nameMatcher;

public class RunCommandCompleter implements CommandCompleter {

    private RunCommand runCommand;

    public void setCommand( RunCommand runCommand ) {
        this.runCommand = runCommand;
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        String prefix = buffer.substring( 0, cursor );
        int pathStartsIndex = prefix.lastIndexOf( ' ' ) + 1;
        String currentPath = prefix.substring( pathStartsIndex );
        int lastSlashIndex = currentPath.lastIndexOf( File.separatorChar );

        Path dir;
        String filePrefix;
        if ( lastSlashIndex > 0 ) {
            String dirPath = currentPath.substring( 0, lastSlashIndex );
            dir = runCommand.getWorkingDirectory().resolve( dirPath );
            filePrefix = dirPath + File.separatorChar;
        } else {
            dir = runCommand.getWorkingDirectory();
            filePrefix = "";
        }

        String[] files = dir.toFile().list();
        if ( files != null ) {
            return new Completer( Stream.of( files ).map( it -> filePrefix + it ) )
                    .complete( buffer, cursor, candidates );
        }
        return -1;
    }

    private static class Completer extends BaseCompleter {
        Completer( Stream<String> options ) {
            super( nameMatcher( "run", anyLevelMatcher(
                    alternativeMatchers( () ->
                            options.map( CompletionMatcher::nameMatcher ) ) ) ) );
        }
    }

}
