package com.athaydes.osgiaas.autocomplete.impl

import com.athaydes.osgiaas.autocomplete.Autocompleter
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Unroll
class OsgiaasAutocompleterSpec extends Specification {

    @Subject
    final Autocompleter completer = new OsgiaasAutocompleter()

    def "Can complete text similar to the best IDEs available"() {
        when: 'completions are requested for #text with options #options'
        def result = completer.completionsFor( text, options )

        then: 'the expected completions are provided: #expectedCompletions'
        result == expectedCompletions

        where:
        text      | options                                  | expectedCompletions
        ''        | [ ]                                      | [ ]
        'a'       | [ ]                                      | [ ]
        ''        | [ '' ]                                   | [ '' ]
        'a'       | [ 'a' ]                                  | [ 'a' ]
        'a'       | [ 'ab' ]                                 | [ 'ab' ]
        'a'       | [ 'ab', 'ac' ]                           | [ 'ab', 'ac' ]
        ''        | [ 'ab', 'ac' ]                           | [ 'ab', 'ac' ]
        'ax'      | [ 'ab', 'ac' ]                           | [ ]
        'ab'      | [ 'ab', 'ac' ]                           | [ 'ab' ]
        'pr'      | [ 'private', 'public', 'protected' ]     | [ 'private', 'protected' ]
        'publ'    | [ 'private', 'public', 'protected' ]     | [ 'public' ]

        'Ax'      | [ 'Ab', 'Ac' ]                           | [ ]
        'Ab'      | [ 'Ab', 'Ac' ]                           | [ 'Ab' ]
        'Pr'      | [ 'Private', 'Public', 'Protected' ]     | [ 'Private', 'Protected' ]
        'Publ'    | [ 'Private', 'Public', 'Protected' ]     | [ 'Public' ]

        'aB'      | [ 'B' ]                                  | [ ]
        'aB'      | [ 'allBall' ]                            | [ 'allBall' ]
        'aB'      | [ 'allBall', 'allCool' ]                 | [ 'allBall' ]
        'aB'      | [ 'aB', 'allCool' ]                      | [ 'aB' ]
        'takeTSt' | [ 'takeTheString', 'takeBool' ]          | [ 'takeTheString' ]

        'FooF'    | [ 'FooFighter', 'Foofighter' ]           | [ 'FooFighter' ]
        'FooFI'   | [ 'FooFighter', 'Foofighter' ]           | [ ]
        'FooFI'   | [ 'FooFighterInRio', 'FoofighterNotIn' ] | [ 'FooFighterInRio' ]
        'FooFIR'  | [ 'FooFighterInRio', 'FoofighterNotIn' ] | [ 'FooFighterInRio' ]
        'FooFIRs' | [ 'FooFighterInRio', 'FoofighterNotIn' ] | [ ]
        'FooFIRS' | [ 'FooFighterInRio', 'FoofighterNotIn' ] | [ ]
    }

}
