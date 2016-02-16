package com.athaydes.osgiaas.api.cli;

import java.util.List;
import java.util.function.Function;

/**
 * A function whose purpose is to modify CLI commands before they get to be executed.
 * <p>
 * Any Service registered with this interface will be used by the Cli service to modify
 * commands sent by the user before executing the command.
 * <p>
 * The function should return a List to allow modifiers to break the command up into separate
 * commands.
 * <p>
 * When a command is broken up by any modifier, all sub-commands are again modified by
 * all registered modifiers.
 */
@FunctionalInterface
public interface CommandModifier extends Function<String, List<String>> {
}
