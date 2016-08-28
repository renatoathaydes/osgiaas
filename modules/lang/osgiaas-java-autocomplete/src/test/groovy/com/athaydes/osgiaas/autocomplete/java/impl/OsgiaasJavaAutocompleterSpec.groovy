package com.athaydes.osgiaas.autocomplete.java.impl

import com.athaydes.osgiaas.autocomplete.Autocompleter
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Array
import java.lang.reflect.Method

@Unroll
class OsgiaasJavaAutocompleterSpec extends Specification {

    @Shared
    @Subject
    def completer = new OsgiaasJavaAutocompleter(
            Autocompleter.getStartWithAutocompleter(),
            DefaultContext.instance )

    def "Can find out the correct index to start looking at the code fragment"() {
        when: 'the completer gets the start index to complete a code fragment'
        def startIndex = completer.indexToStartCompletion( codeFragment )

        then: 'the index returned should be after the last code delimiter, not including trailing whitespaces'
        codeFragment.substring( startIndex ) == expectedCode

        where:
        codeFragment                              | expectedCode
        ''                                        | ''
        ' '                                       | ' '
        'a.'                                      | 'a.'
        'class A {} a.'                           | 'a.'
        'class A {} a. '                          | 'a. '
        'class A {} a.b().c.'                     | 'a.b().c.'
        'class A {} a.b().c  '                    | 'a.b().c  '
        '  a.'                                    | 'a.'
        'class A {void main() {\na.'              | 'a.'
        '  "abc".toString(); "cde".toS'           | '"cde".toS'
        'new A().hi'                              | 'new A().hi'
        'class A{void main() { return new A().hi' | 'new A().hi'
    }

    def "Can complete simple text based on bindings and Java keywords"() {
        when: 'completions are requested for #text with bindings #bindings'
        def result = completer.completionsFor( text, bindings )

        def allExpectedCompletions = useAllTopLevelCompletions ?
                completer.topLevelCompletions( [ ] ) + expectedCompletions :
                expectedCompletions

        then: 'the expected completions are provided: #expectedCompletions'
        result.completions == allExpectedCompletions
        result.completionIndex == expectedIndex

        where:
        text   | bindings                    | useAllTopLevelCompletions | expectedCompletions        | expectedIndex
        ''     | [ : ]                       | true                      | [ ]                        | 0
        ''     | [ hi: 1, bye: 2 ]           | true                      | [ 'hi', 'bye' ]            | 0
        ''     | [ _one: 1 ]                 | true                      | [ '_one' ]                 | 0
        'publ' | [ : ]                       | false                     | [ 'public' ]               | 0
        'publ' | [ publicize: true ]         | false                     | [ 'public', 'publicize' ]  | 0
        'publ' | [ publicize: true, pub: 1 ] | false                     | [ 'public', 'publicize' ]  | 0
        'pr'   | [ : ]                       | false                     | [ 'private', 'protected' ] | 0
        'a pr' | [ : ]                       | false                     | [ 'private', 'protected' ] | 2
    }

    def "Can complete second-level text based on the type of the first-level word"() {
        when: 'completions are requested for #text with bindings #bindings'
        def result = completer.completionsFor( text, bindings )

        then: 'the expected completions are provided: #expectedCompletions'
        result.completions == expectedCompletions
        result.completionIndex == expectedIndex

        where:
        text               | bindings     | expectedCompletions            | expectedIndex
        'hi.toS'           | [ hi: 'Hi' ] | [ 'toString()' ]               | 3
        '"hi".toS'         | [ : ]        | [ 'toString()' ]               | 5
        'ho.getF'          | [ ho: List ] | [ 'getField(', 'getFields()' ] | 3
        'return ho.getF'   | [ ho: List ] | [ 'getField(', 'getFields()' ] | 10
        'Integer.class.ca' | [ : ]        | [ 'cast(' ]                    | 14
        'new int[0].len'   | [ : ]        | [ 'length' ]                   | 11
        'new int[0].get'   | [ : ]        | [ 'getClass()' ]               | 11
        'Integer.MAX'      | [ : ]        | [ 'MAX_VALUE' ]                | 8
        '20.MAX'           | [ : ]        | [ ]                            | -1
        'new Object().TY'  | [ : ]        | [ ]                            | -1
    }

    def "Can complete third-level text based on the type of the previous words"() {
        when: 'completions are requested for #text with bindings #bindings'
        def result = completer.completionsFor( text, bindings )

        then: 'the expected completions are provided: #expectedCompletions'
        result.completions as Set == expectedCompletions as Set
        result.completionIndex == expectedIndex

        where:
        text                | bindings     | expectedCompletions                 | expectedIndex
        'hi.toString().toU' | [ hi: 'Hi' ] | [ 'toUpperCase()', 'toUpperCase(' ] | 14
        'hi.getBytes().le'  | [ hi: 'Hi' ] | [ 'length' ]                        | 14
        'hi.toString().'    | [ hi: 'Hi' ] | [ 'charAt(', 'chars()', 'codePointAt(',
                                               'codePointBefore(', 'codePointCount(', 'codePoints()', 'compareTo(',
                                               'compareTo(', 'compareToIgnoreCase(', 'concat(', 'contains(',
                                               'contentEquals(', 'contentEquals(',
                                               'endsWith(', 'equals(', 'equalsIgnoreCase(',
                                               'getBytes(', 'getBytes()', 'getChars(', 'getClass()', 'hashCode()',
                                               'indexOf(', 'intern()', 'isEmpty()', 'lastIndexOf(',
                                               'lastIndexOf(', 'length()', 'matches(', 'notify()', 'notifyAll()',
                                               'offsetByCodePoints(', 'regionMatches(', 'replace(', 'replaceAll(',
                                               'replaceFirst(', 'split(', 'startsWith(', 'subSequence(', 'substring(',
                                               'toCharArray()', 'toLowerCase(', 'toLowerCase()', 'toString()',
                                               'toUpperCase(', 'toUpperCase()', 'trim()',
                                               'wait(', 'wait()' ]               | 14
    }

    def "Can find out the type of the first part of the input"() {
        when: 'the type of the first part of "#text" is requested'
        def result = completer.findTypeOfFirstPart( text )

        then: 'the correct type is provided: #expectedType'
        result.type == expectedType
        result.isStatic == expectedStatic

        where:
        text                | expectedType | expectedStatic
        'new Object().g'    | Object       | false
        '"hello"'           | String       | false
        'new String().to'   | String       | false
        'Boolean.class'     | Class        | false
        'Boolean.class.get' | Class        | false
        '2'                 | void         | false // all primitive types can just return Void
        "'a'"               | void         | false
        '4L'                | void         | false
        'new int[0].le'     | Array        | false
    }

    def "Can find out the type of the first part of the input using imports"() {
        given: 'A completer with a few known imports'
        def completer = new OsgiaasJavaAutocompleter(
                Autocompleter.getDefaultAutocompleter(),
                Stub( JavaAutocompleteContext ) {
                    getImports() >> [ 'spock.lang.Subject', 'java.util.List', 'java.lang.reflect.*' ]
                    getMethodBody( _ as String ) >> { String x -> x }
                } )

        when: 'the type of the first part of "#text" is requested'
        def result = completer.findTypeOfFirstPart( text )

        then: 'the correct type is provided: #expectedType'
        result.type == expectedType
        result.isStatic == expectedStatic

        where:
        text                | expectedType | expectedStatic
        'new Subject().abc' | Subject      | false
        'new List(abc).def' | List         | false
        'Subject.abc'       | Subject      | true
        'String.ind'        | String       | true // imported with java.lang implicitly
        'Array.l'           | Array        | true // imported with java.lang.reflect.*
        'Method.a'          | Method       | true // imported with java.lang.reflect.*

    }

    def "Knows how to breakup text into last type and text to complete"() {
        when:
        def result = completer.lastTypeAndTextToComplete( codeParts as LinkedList, bindings )

        then:
        result.lastType.type == expectedType
        result.textToComplete == expectedTextToComplete

        where:
        codeParts                    | bindings   | expectedType | expectedTextToComplete
        [ '"hi"', 'to' ]             | [ : ]      | String       | 'to'
        [ 'hi', 'to' ]               | [ hi: '' ] | String       | 'to'
        [ 'n', 'to' ]                | [ n: 0 ]   | Integer      | 'to'
        [ '10', '' ]                 | [ : ]      | void         | ''
        [ 'hi', 'toString()', '' ]   | [ hi: '' ] | String       | ''
        [ 'hi', 'getBytes()', 'le' ] | [ hi: '' ] | Array        | 'le'
    }

}