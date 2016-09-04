package com.athaydes.osgiaas.cli.frege;

import com.athaydes.osgiaas.cli.CommandCompleter;

import java.util.List;

public class FregeCommandCompleter implements CommandCompleter {
    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
        return -1;
    }
}
