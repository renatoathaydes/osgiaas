package com.athaydes.osgiaas.cli.core.completer

import spock.lang.Specification
import spock.lang.Unroll

class ListResourcesCompleterSpec extends Specification {

    @Unroll
    def "Must know how to extract the relevant part of a suggestion when filtering suggestions"() {
        when: 'We extract the relevant suggestion part from a suggestion'
        def result = ListResourceCompleter.relevantSuggestionPart( suggestion )

        then: 'We get the relevant part that can be used by the filter'
        result == relevantPart

        where:
        suggestion                | relevantPart
        ''                        | ''
        'a'                       | 'a'
        'a/'                      | 'a/'
        'a/b'                     | 'b'
        'a/b/'                    | 'b/'
        'META-INF/services/hello' | 'hello'
        'META-INF/services/data/' | 'data/'
    }

}
