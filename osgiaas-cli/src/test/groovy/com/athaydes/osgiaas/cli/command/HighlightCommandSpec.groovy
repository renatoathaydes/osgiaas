package com.athaydes.osgiaas.cli.command

import spock.lang.Specification
import spock.lang.Unroll

import static com.athaydes.osgiaas.api.ansi.AnsiColor.BLACK
import static com.athaydes.osgiaas.api.ansi.AnsiColor.CYAN
import static com.athaydes.osgiaas.api.ansi.AnsiColor.DEFAULT_BG
import static com.athaydes.osgiaas.api.ansi.AnsiColor.GREEN
import static com.athaydes.osgiaas.api.ansi.AnsiColor.RED
import static com.athaydes.osgiaas.api.ansi.AnsiColor.WHITE
import static com.athaydes.osgiaas.api.ansi.AnsiColor._BLUE
import static com.athaydes.osgiaas.api.ansi.AnsiColor._GREEN
import static com.athaydes.osgiaas.api.ansi.AnsiColor._PURPLE
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.BLINK
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.ITALIC
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.RAPID_BLINK
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.UNDERLINE

class HighlightCommandSpec extends Specification {

    @Unroll
    def "HighlightCall is created as appropriate"() {
        when: 'A highlightCall is created from an example #line'
        def result = HighlightCommand.highlightCall( "highlight $args regex input" )

        then: 'all values are correctly read'
        result != null
        result.argumentsGiven == expectedArgumentsGiven
        result.colors.toList() == expectedColors
        result.modifiers.toList() == expectedModifiers

        where:
        args                       | expectedArgumentsGiven | expectedColors        | expectedModifiers
        ''                         | 0                      | [ DEFAULT_BG ]        | [ ]
        '-F red'                   | 1                      | [ DEFAULT_BG, RED ]   | [ ]
        '-B blue'                  | 1                      | [ _BLUE ]             | [ ]
        '-F white -B green'        | 2                      | [ _GREEN, WHITE ]     | [ ]
        '-B purple -F cyan'        | 2                      | [ _PURPLE, CYAN ]     | [ ]
        '-F cyan+italic'           | 1                      | [ DEFAULT_BG, CYAN ]  | [ ITALIC ]
        '-F black+blink+underline' | 1                      | [ DEFAULT_BG, BLACK ] | [ BLINK, UNDERLINE ]
        '-F green+rapid_blink'     | 1                      | [ DEFAULT_BG, GREEN ] | [ RAPID_BLINK ]
        '-B purple -F cyan+blink'  | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]
        '-F cyan+blink -B purple'  | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]
    }

    @Unroll
    def "HighlightCall can be created from short modifier names"() {
        when: 'A highlightCall is created from an example #line using short names'
        def result = HighlightCommand.highlightCall( "highlight $args regex input" )

        then: 'all values are correctly read'
        result != null
        result.argumentsGiven == expectedArgumentsGiven
        result.colors.toList() == expectedColors
        result.modifiers.toList() == expectedModifiers

        where:
        args                  | expectedArgumentsGiven | expectedColors        | expectedModifiers
        '-F cyan+i'           | 1                      | [ DEFAULT_BG, CYAN ]  | [ ITALIC ]
        '-F black+b+u'        | 1                      | [ DEFAULT_BG, BLACK ] | [ BLINK, UNDERLINE ]
        '-F green+rb'         | 1                      | [ DEFAULT_BG, GREEN ] | [ RAPID_BLINK ]
        '-B purple -F cyan+b' | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]
        '-F cyan+b -B purple' | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]
    }

}
