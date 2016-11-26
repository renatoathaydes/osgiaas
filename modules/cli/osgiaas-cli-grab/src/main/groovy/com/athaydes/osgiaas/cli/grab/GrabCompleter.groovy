package com.athaydes.osgiaas.cli.grab

import com.athaydes.osgiaas.cli.completer.BaseCompleter

import static com.athaydes.osgiaas.cli.completer.CompletionMatcher.nameMatcher

//@CompileStatic
class GrabCompleter extends BaseCompleter {

    GrabCompleter() {
        super( nameMatcher( 'grab',
                nameMatcher( GrabCommand.ADD_REPO ), nameMatcher( GrabCommand.LIST_REPOS ),
                nameMatcher( GrabCommand.REMOVE_REPO ), nameMatcher( GrabCommand.VERBOSE )
        ) )
    }

}
