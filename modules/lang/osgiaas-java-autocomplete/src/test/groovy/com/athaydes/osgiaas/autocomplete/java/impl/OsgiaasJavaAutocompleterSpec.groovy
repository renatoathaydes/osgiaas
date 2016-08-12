package com.athaydes.osgiaas.autocomplete.java.impl

import com.athaydes.osgiaas.autocomplete.Autocompleter
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Unroll
class OsgiaasJavaAutocompleterSpec extends Specification {

    @Shared
    @Subject
    def completer = new OsgiaasJavaAutocompleter(
            Autocompleter.getStartWithAutocompleter(),
            DefaultContext.instance )

    def "Can complete simple text based on bindings and Java keywords"() {
        when: 'completions are requested for #text with bindings #bindings'
        def result = completer.completionsFor( text, bindings )

        def allExpectedCompletions = useAllTopLevelCompletions ?
                completer.topLevelCompletions( [ ] ) + expectedCompletions :
                expectedCompletions

        then: 'the expected completions are provided: #expectedCompletions'
        result.completions == allExpectedCompletions
        result.completionIndex == 0

        where:
        text   | bindings                    | useAllTopLevelCompletions | expectedCompletions
        ''     | [ : ]                       | true                      | [ ]
        ''     | [ hi: 1, bye: 2 ]           | true                      | [ 'hi', 'bye' ]
        ''     | [ _one: 1 ]                 | true                      | [ '_one' ]
        'publ' | [ : ]                       | false                     | [ 'public' ]
        'publ' | [ publicize: true ]         | false                     | [ 'public', 'publicize' ]
        'publ' | [ publicize: true, pub: 1 ] | false                     | [ 'public', 'publicize' ]
        'pr'   | [ : ]                       | false                     | [ 'private', 'protected' ]
    }

    def "Can complete second-level text based on the type of the first-level word"() {
        when: 'completions are requested for #text with bindings #bindings'
        def result = completer.completionsFor( text, bindings )

        then: 'the expected completions are provided: #expectedCompletions'
        result.completions == expectedCompletions
        result.completionIndex == expectedIndex

        where:
        text       | bindings     | expectedCompletions            | expectedIndex
        'hi.toS'   | [ hi: 'Hi' ] | [ 'toString()' ]               | 3
        '"hi".toS' | [ : ]        | [ 'toString()' ]               | 5
        'ho.getF'  | [ ho: List ] | [ 'getField(', 'getFields()' ] | 3
    }

    def "Can find out the type of the first part of the input"() {
        when: 'the type of the first part of "#text" is requested'
        def result = completer.findTypeOfFirstPart( text )

        then: 'the correct type is provided: #expectedType'
        result == expectedType

        where:
        text                | expectedType
        'new Object().g'    | Object
        '"hello"'           | String
        'new String().to'   | String
        'Boolean.class'     | Class
        'Boolean.class.get' | Class
        '2'                 | Void // all primivite types can just return Void
        "'a'"               | Void
        '4L'                | Void
    }

    def "Can find out the type of the first part of the input using imports"() {
        given: 'A completer with a few known imports'
        def completer = new OsgiaasJavaAutocompleter(
                Autocompleter.getDefaultAutocompleter(),
                Stub( JavaAutocompleteContext ) {
                    getImports() >> [ 'spock.lang.Subject', 'java.util.List' ]
                    getMethodBody( _ as String ) >> { String x -> x }
                } )

        when: 'the type of the first part of "#text" is requested'
        def result = completer.findTypeOfFirstPart( text )

        then: 'the correct type is provided: #expectedType'
        result == expectedType

        where:
        text                | expectedType
        'new Subject().abc' | Subject
        'new List(abc).def' | List
    }

    def "Knows how to breakup text into last type and text to complete"() {
        when:
        def result = completer.lastTypeAndTextToComplete( codeParts as LinkedList, bindings )

        then:
        result.lastType == expectedType
        result.textToComplete == expectedTextToComplete

        where:
        codeParts                  | bindings   | expectedType | expectedTextToComplete
        [ '"hi"', 'to' ]           | [ : ]      | String       | 'to'
        [ 'hi', 'to' ]             | [ hi: '' ] | String       | 'to'
        [ 'n', 'to' ]              | [ n: 0 ]   | Integer      | 'to'
        [ '10', '' ]               | [ : ]      | Void         | ''
        [ 'hi', 'toString()', '' ] | [ hi: '' ] | String       | ''
        //[ 'hi', 'getBytes()', 'length', '' ] | [ hi: '' ] | int          | ''
    }

}