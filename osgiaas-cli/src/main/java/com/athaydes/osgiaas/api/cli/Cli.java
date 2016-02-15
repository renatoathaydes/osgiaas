package com.athaydes.osgiaas.api.cli;

/**
 * The Cli (command-line-interface) service.
 */
public interface Cli {

    void start();

    void stop();

    void setPrompt( String prompt );

    void setPromptColor( AnsiColor color );

}
