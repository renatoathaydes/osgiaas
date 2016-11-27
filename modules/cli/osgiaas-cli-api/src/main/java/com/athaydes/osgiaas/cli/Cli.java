package com.athaydes.osgiaas.cli;

import com.athaydes.osgiaas.api.ansi.AnsiColor;

/**
 * The Cli (command-line-interface) service.
 */
public interface Cli {

    /**
     * Start the CLI.
     */
    void start();

    /**
     * Stop the CLI.
     */
    void stop();

    /**
     * Set the CLI prompt.
     *
     * @param prompt CLI prompt.
     */
    void setPrompt( String prompt );

    /**
     * Set the CLI prompt's color.
     *
     * @param color of the prompt.
     */
    void setPromptColor( AnsiColor color );

    /**
     * Set the error output's color.
     *
     * @param color of the error output.
     */
    void setErrorColor( AnsiColor color );

    /**
     * Set the regular output's color.
     *
     * @param color of the regular output.
     */
    void setTextColor( AnsiColor color );

    /**
     * Clear the CLI screen.
     */
    void clearScreen();

}
