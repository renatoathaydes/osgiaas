package com.athaydes.osgiaas.autocomplete.java.impl

import com.athaydes.osgiaas.autocomplete.java.ResultType
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class DefaultJavaStatementParserSpec extends Specification {

    @Subject
    @Shared
    def parser = new DefaultJavaStatementParser()

    @Unroll
    def "Can parse valid Java statements"() {
        when:
        def result = parser.parseStatements( javaStatements, [ ] )

        then:
        result == expectedResult

        where:
        javaStatements                | expectedResult
        [ ]                           | [ : ]
        [ 'int i = 0;' ]              | [ i: new ResultType( int, false ) ]
        [ 'long l = 0L;' ]            | [ l: new ResultType( long, false ) ]
        [ 'float f = 0f;' ]           | [ f: new ResultType( float, false ) ]
        [ 'short s = 0;' ]            | [ s: new ResultType( short, false ) ]
        [ 'byte b = 0;' ]             | [ b: new ResultType( byte, false ) ]
        [ 'boolean is = true;' ]      | [ is: new ResultType( boolean, false ) ]
        [ "char c = 'c';" ]           | [ c: new ResultType( char, false ) ]
        [ 'String s = "";' ]          | [ s: new ResultType( String, false ) ]
        [ 'Class c = int.class;' ]    | [ c: new ResultType( Class, false ) ]
        [ 'Class<?> c = int.class;' ] | [ c: new ResultType( Class, false ) ]
    }

    @Unroll
    def "Can parse valid Java statements using imports"() {
        when:
        def result = parser.parseStatements( javaStatements, [ FirstType.name, OtherType.name ] )

        then:
        result == expectedResult

        where:
        javaStatements                           | expectedResult
        [ 'FirstType first = new FirstType();' ] | [ first: new ResultType( FirstType, false ) ]
        [ 'OtherType other= new FirstType();' ]  | [ other: new ResultType( OtherType, false ) ]
        [ "char c = 'c';" ]                      | [ c: new ResultType( char, false ) ]
        [ 'String s = "";' ]                     | [ s: new ResultType( String, false ) ]
    }

}

class FirstType {}

class OtherType {}
