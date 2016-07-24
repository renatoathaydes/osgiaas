package com.athaydes.osgiaas.cli.core.command

import com.athaydes.osgiaas.api.stream.LineOutputStream
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

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
        given: 'A mocked out PrintStream'
        def errors = [ ]
        def errStream = new PrintStream( new LineOutputStream( errors.&add, Stub( AutoCloseable ) ) )

        when: 'A highlightCall is created from an example #line'
        def result = new HighlightCommand()
                .highlightCall( "highlight $args regex input", errStream )

        then: 'all values are correctly read'
        result != null
        result.colors.toList() == expectedColors
        result.modifiers.toList() == expectedModifiers
        ( result.pattern.flags() & Pattern.CASE_INSENSITIVE ) >= ( caseInsensitive ? 1 : 0 )

        and: 'No error is reported'
        errors.empty

        where:
        args                         | expectedArgumentsGiven | expectedColors        | expectedModifiers    | caseInsensitive
        ''                           | 0                      | [ DEFAULT_BG ]        | [ ]                  | false
        '-f red'                     | 1                      | [ DEFAULT_BG, RED ]   | [ ]                  | false
        '-b blue'                    | 1                      | [ _BLUE ]             | [ ]                  | false
        '-i -b blue'                 | 1                      | [ _BLUE ]             | [ ]                  | true
        '-f white -b green'          | 2                      | [ _GREEN, WHITE ]     | [ ]                  | false
        '-b purple -f cyan'          | 2                      | [ _PURPLE, CYAN ]     | [ ]                  | false
        '-f cyan+italic'             | 1                      | [ DEFAULT_BG, CYAN ]  | [ ITALIC ]           | false
        '-f black+blink+underline'   | 1                      | [ DEFAULT_BG, BLACK ] | [ BLINK, UNDERLINE ] | false
        '-f green+rapid_blink'       | 1                      | [ DEFAULT_BG, GREEN ] | [ RAPID_BLINK ]      | false
        '-b purple -f cyan+blink'    | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]            | false
        '-b purple -f cyan+blink -i' | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]            | true
        '-f cyan+blink -b purple'    | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]            | false
        '-f cyan+blink -i -b purple' | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]            | true
    }

    @Unroll
    def "HighlightCall can be created from short modifier names"() {
        given: 'A mocked out PrintStream'
        def errors = [ ]
        def errStream = new PrintStream( new LineOutputStream( errors.&add, Stub( AutoCloseable ) ) )

        when: 'A highlightCall is created from an example #line using short names'
        def result = new HighlightCommand()
                .highlightCall( "highlight $args regex input", errStream )

        then: 'all values are correctly read'
        result != null
        result.colors.toList() == expectedColors
        result.modifiers.toList() == expectedModifiers

        and: 'No error is reported'
        errors.empty

        where:
        args                  | expectedArgumentsGiven | expectedColors        | expectedModifiers
        '-f cyan+i'           | 1                      | [ DEFAULT_BG, CYAN ]  | [ ITALIC ]
        '-f black+b+u'        | 1                      | [ DEFAULT_BG, BLACK ] | [ BLINK, UNDERLINE ]
        '-f green+rb'         | 1                      | [ DEFAULT_BG, GREEN ] | [ RAPID_BLINK ]
        '-b purple -f cyan+b' | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]
        '-f cyan+b -b purple' | 2                      | [ _PURPLE, CYAN ]     | [ BLINK ]
    }

}
