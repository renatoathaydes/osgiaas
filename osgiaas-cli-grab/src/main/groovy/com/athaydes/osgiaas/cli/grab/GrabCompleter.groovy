package com.athaydes.osgiaas.cli.grab

import com.athaydes.osgiaas.api.cli.completer.BaseCompleter
import groovy.transform.CompileStatic

import static com.athaydes.osgiaas.api.cli.completer.CompletionMatcher.nameMatcher
import static com.athaydes.osgiaas.cli.grab.GrabCommand.ADD_REPO
import static com.athaydes.osgiaas.cli.grab.GrabCommand.LIST_REPOS
import static com.athaydes.osgiaas.cli.grab.GrabCommand.REMOVE_REPO
import static com.athaydes.osgiaas.cli.grab.GrabCommand.VERBOSE

@CompileStatic
class GrabCompleter extends BaseCompleter {

    GrabCompleter() {
        super( nameMatcher( 'grab',
                nameMatcher( ADD_REPO ), nameMatcher( LIST_REPOS ),
                nameMatcher( REMOVE_REPO ), nameMatcher( VERBOSE )
        ) )
    }

}
