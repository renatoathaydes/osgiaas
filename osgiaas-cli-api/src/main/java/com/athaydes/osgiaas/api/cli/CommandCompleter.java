package com.athaydes.osgiaas.api.cli;

import java.util.List;

/**
 * A CLI Command completer.
 */
@FunctionalInterface
public interface CommandCompleter {

    /**
     * Populates <i>candidates</i> with a list of possible completions for the <i>buffer</i>.
     * <p>
     * The <i>candidates</i> list will not be sorted before being displayed to the user: thus, the
     * complete method should sort the {@link List} before returning.
     *
     * @param buffer     The buffer
     * @param cursor     The current position of the cursor in the <i>buffer</i>
     * @param candidates The {@link List} of candidates to populate
     * @return The index of the <i>buffer</i> for which the completion will be relative
     */
    int complete( String buffer, int cursor, List<CharSequence> candidates );
}
