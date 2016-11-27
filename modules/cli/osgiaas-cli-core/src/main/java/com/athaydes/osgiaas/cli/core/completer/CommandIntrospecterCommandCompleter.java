package com.athaydes.osgiaas.cli.core.completer;

import com.athaydes.osgiaas.cli.completer.CompletionMatcher;

import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.core.command.CommandIntrospecterCommand.VERBOSE_ARG;

public class CommandIntrospecterCommandCompleter extends AbstractTakesCommandsAsArgsCompleter {

    @Override
    protected String commandName() {
        return "ci";
    }

    @Override
    protected Stream<CompletionMatcher> matchers() {
        return Stream.of( CompletionMatcher.alternativeMatchers( () -> Stream.concat(
                super.matchers(),
                Stream.of( CompletionMatcher.nameMatcher( VERBOSE_ARG, super::matchers ) ) ) ) );
    }
}
