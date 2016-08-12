package com.athaydes.osgiaas.autocomplete.java;

import java.util.List;

/**
 * Result of requesting Java autocompletion.
 */
public class JavaAutocompleterResult {
    private final List<String> completions;
    private final int completionIndex;

    public JavaAutocompleterResult( List<String> completions, int completionIndex ) {
        this.completions = completions;
        this.completionIndex = completionIndex;
    }

    public List<String> getCompletions() {
        return completions;
    }

    public int getCompletionIndex() {
        return completionIndex;
    }
}
