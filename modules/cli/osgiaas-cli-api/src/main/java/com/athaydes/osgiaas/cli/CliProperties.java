package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.ansi.AnsiColor;

/**
 * Representation of all Cli properties which can be set by other services.
 */
public interface CliProperties {

    /**
     * @return names of all available commands.
     */
    String[] availableCommands();

    /**
     * @return the current CLI prompt.
     */
    String getPrompt();

    /**
     * @return the command being 'used', if any, or the empty String if no command is being used.
     * @see KnowsCommandBeingUsed
     */
    String commandBeingUsed();

    /**
     * @return the current color of the CLI prompt.
     */
    AnsiColor getPromptColor();

    /**
     * @return the current color of the CLI regular output.
     */
    AnsiColor getTextColor();

    /**
     * @return the current color of the CLI error output.
     */
    AnsiColor getErrorColor();

}
