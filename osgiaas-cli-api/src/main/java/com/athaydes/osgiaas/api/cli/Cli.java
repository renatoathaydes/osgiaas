package com.athaydes.osgiaas.api.cli;

import com.athaydes.osgiaas.api.ansi.AnsiColor;

/**
 * The Cli (command-line-interface) service.
 */
public interface Cli {

    void start();

    void stop();

    void setPrompt( String prompt );

    void setPromptColor( AnsiColor color );

    void setErrorColor( AnsiColor color );

    void setTextColor( AnsiColor color );

    void clearScreen();

}
