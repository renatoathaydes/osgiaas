package com.athaydes.osgiaas.cli;

/**
 * The OSGiaaS-CLI supports the concept of <em>using a command</em>, which means a user may choose to temporarily
 * make the CLI pass anything the user types as an argument to a specific command (including even some options),
 * instead of assuming everything the user types is a command.
 */
public interface KnowsCommandBeingUsed {

    /**
     * @return the current command being used, or the empty String if no command is being used.
     */
    String using();

}
