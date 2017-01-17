package com.athaydes.osgiaas.cli.core.command

import com.athaydes.osgiaas.api.ansi.AnsiColor
import com.athaydes.osgiaas.api.stream.LineOutputStream
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

import static com.athaydes.osgiaas.api.ansi.AnsiColor.BKG_BLUE
import static com.athaydes.osgiaas.api.ansi.AnsiColor.BKG_GREEN
import static com.athaydes.osgiaas.api.ansi.AnsiColor.BKG_PURPLE
import static com.athaydes.osgiaas.api.ansi.AnsiColor.BLACK
import static com.athaydes.osgiaas.api.ansi.AnsiColor.CYAN
import static com.athaydes.osgiaas.api.ansi.AnsiColor.DEFAULT_BG
import static com.athaydes.osgiaas.api.ansi.AnsiColor.GREEN
import static com.athaydes.osgiaas.api.ansi.AnsiColor.RED
import static com.athaydes.osgiaas.api.ansi.AnsiColor.WHITE
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.BLINK
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.BOLD
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.RAPID_BLINK
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.REVERSE
import static com.athaydes.osgiaas.api.ansi.AnsiModifier.UNDERLINE

class HighlightCommandSpec extends Specification {

    @Unroll
    def "HighlightCall is created as appropriate"() {
        given: 'A mocked out PrintStream'
        def errors = [ ]
        def errStream = new PrintStream( new LineOutputStream( errors.&add, Stub( AutoCloseable ) ) )

        when: 'A highlightCall is created from an example #line'
        final result = new HighlightCommand()
                .highlightCall( "highlight $args regex input", errStream )

        then: 'all values are correctly read'
        !errors && result != null
        result.backEscape == AnsiColor.backColorEscapeCode( expectedBkg.name() )
        result.foreEscape == ( expectedFore ? AnsiColor.foreColorEscapeCode( expectedFore.name() ) : '' )
        result.modifiers.toList() == expectedModifiers
        ( result.pattern.flags() & Pattern.CASE_INSENSITIVE ) >= ( caseInsensitive ? 1 : 0 )

        where:
        args                         | expectedBkg | expectedFore | expectedModifiers    | caseInsensitive
        ''                           | DEFAULT_BG  | null         | [ ]                  | false
        '-f red'                     | DEFAULT_BG  | RED          | [ ]                  | false
        '-b blue'                    | BKG_BLUE    | null         | [ ]                  | false
        '-i -b blue'                 | BKG_BLUE    | null         | [ ]                  | true
        '-f white -b green'          | BKG_GREEN   | WHITE        | [ ]                  | false
        '-b purple -f cyan'          | BKG_PURPLE  | CYAN         | [ ]                  | false
        '-f cyan+bold'               | DEFAULT_BG  | CYAN         | [ BOLD ]             | false
        '-f black+blink+underline'   | DEFAULT_BG  | BLACK        | [ BLINK, UNDERLINE ] | false
        '-f green+rapid_blink'       | DEFAULT_BG  | GREEN        | [ RAPID_BLINK ]      | false
        '-b purple -f cyan+blink'    | BKG_PURPLE  | CYAN         | [ BLINK ]            | false
        '-b purple -f cyan+blink -i' | BKG_PURPLE  | CYAN         | [ BLINK ]            | true
        '-f cyan+blink -b purple'    | BKG_PURPLE  | CYAN         | [ BLINK ]            | false
        '-f cyan+blink -i -b purple' | BKG_PURPLE  | CYAN         | [ BLINK ]            | true
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
        !errors && result != null
        result.backEscape == AnsiColor.backColorEscapeCode( expectedBkg.name() )
        result.foreEscape == ( expectedFore ? AnsiColor.foreColorEscapeCode( expectedFore.name() ) : '' )
        result.modifiers.toList() == expectedModifiers

        and: 'No error is reported'
        errors.empty

        where:
        args                   | expectedBkg | expectedFore | expectedModifiers
        '-f cyan+b'            | DEFAULT_BG  | CYAN         | [ BOLD ]
        '-f black+bl+u'        | DEFAULT_BG  | BLACK        | [ BLINK, UNDERLINE ]
        '-f green+rb'          | DEFAULT_BG  | GREEN        | [ RAPID_BLINK ]
        '-b purple -f cyan+bl' | BKG_PURPLE  | CYAN         | [ BLINK ]
        '-f cyan+rv -b purple' | BKG_PURPLE  | CYAN         | [ REVERSE ]
    }

}
