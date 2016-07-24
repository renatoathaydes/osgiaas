package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;

import java.util.List;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.alternativeMatchers;
import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.nameMatcher;

public class JavaCompleter implements CommandCompleter {

    private final BaseCompleter argsMatcher = new BaseCompleter( nameMatcher( "java",

            nameMatcher( JavaCommand.CLASS_ARG, JavaCompleter::forClassArg ),
            nameMatcher( JavaCommand.RESET_CODE_ARG ),
            nameMatcher( JavaCommand.RESET_ALL_ARG ),
            nameMatcher( JavaCommand.SHOW_ARG ),
            nameMatcher( "return" ),
            nameMatcher( "out" ),
            nameMatcher( "err" ),
            nameMatcher( "ctx" ),
            nameMatcher( "binding" ),
            nameMatcher( "System" ),
            nameMatcher( "String" )
    ) );

    private static Stream<CompletionMatcher> forClassArg() {
        return Stream.of(
                nameMatcher( "class" ), nameMatcher( "interface" ),
                nameMatcher( "public", alternativeMatchers(
                        nameMatcher( "class" ), nameMatcher( "interface" ) ) ) );
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        return argsMatcher.complete( buffer, cursor, candidates );
    }

}
