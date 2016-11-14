package com.athaydes.osgiaas.cli.java;

import com.athaydes.osgiaas.autocomplete.Autocompleter;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleter;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleterResult;
import com.athaydes.osgiaas.autocomplete.java.JavaStatementParser;
import com.athaydes.osgiaas.autocomplete.java.ResultType;
import com.athaydes.osgiaas.cli.CommandCompleter;
import com.athaydes.osgiaas.cli.completer.BaseCompleter;
import com.athaydes.osgiaas.cli.completer.CompletionMatcher;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.alternativeMatchers;
import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.nameMatcher;

public class JavaCompleter implements CommandCompleter {

    private JavaCommand javaCommand;

    private final BaseCompleter argsMatcher = new BaseCompleter( nameMatcher( "java",
            nameMatcher( JavaCommand.CLASS_ARG, JavaCompleter::forClassArg ),
            nameMatcher( JavaCommand.RESET_CODE_ARG ),
            nameMatcher( JavaCommand.RESET_ALL_ARG ),
            nameMatcher( JavaCommand.SHOW_ARG )
    ) );

    private static Stream<CompletionMatcher> forClassArg() {
        return Stream.of(
                nameMatcher( "class" ), nameMatcher( "interface" ),
                nameMatcher( "public", alternativeMatchers(
                        nameMatcher( "class" ), nameMatcher( "interface" ) ) ) );
    }

    public void setJavaCommand( JavaCommand javaCommand ) {
        this.javaCommand = javaCommand;
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        if ( !buffer.startsWith( "java " ) ) {
            return -1;
        }

        int startIndex = "java ".length();

        int argCompleterIndex = argsMatcher.complete( buffer, cursor, candidates );

        JavaCode context = getContext();

        Map<String, ResultType> bindings = getVariableBindings( context );

        try {
            bindings.putAll( JavaStatementParser.getDefaultParser()
                    .parseStatements( context.getJavaLines().stream()
                                    .map( it -> it + ";" )
                                    .collect( Collectors.toList() ),
                            context.getImports(),
                            context.getClassLoaderContext().orElse( null ) ) );
        } catch ( ParseException e ) {
            e.printStackTrace();
        }

        JavaAutocompleter autocompleter = JavaAutocompleter.getAutocompleter(
                Autocompleter.getDefaultAutocompleter(),
                context );

        JavaAutocompleterResult completionResult = autocompleter.completionsFor(
                buffer.substring( startIndex, cursor ), bindings );

        candidates.addAll( completionResult.getCompletions() );

        int completionIndex = completionResult.getCompletionIndex();

        return completionIndex >= 0 ?
                completionIndex + startIndex :
                argCompleterIndex;
    }

    private static Map<String, ResultType> getVariableBindings( JavaCode context ) {
        return new HashMap<>( context.getVariables()
                .entrySet().stream()
                .collect( Collectors.toMap(
                        Map.Entry::getKey,
                        it -> new ResultType( it.getValue().getClass(), false ) ) ) );
    }

    private JavaCode getContext() {
        return javaCommand.getAutocompleContext();
    }

}
