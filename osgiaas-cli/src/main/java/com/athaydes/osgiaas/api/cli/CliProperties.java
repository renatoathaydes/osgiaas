package com.athaydes.osgiaas.api.cli;

/**
 * Representation of all Cli properties which can be set by other services.
 */
public interface CliProperties {

    String getPrompt();

    AnsiColor getPromptColor();

    AnsiColor getTextColor();

    AnsiColor getErrorColor();

}
