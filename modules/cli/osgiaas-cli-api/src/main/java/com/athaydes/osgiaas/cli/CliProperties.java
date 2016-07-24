package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.ansi.AnsiColor;

/**
 * Representation of all Cli properties which can be set by other services.
 */
public interface CliProperties {

    String[] availableCommands();

    String getPrompt();

    String commandBeingUsed();

    AnsiColor getPromptColor();

    AnsiColor getTextColor();

    AnsiColor getErrorColor();

}
