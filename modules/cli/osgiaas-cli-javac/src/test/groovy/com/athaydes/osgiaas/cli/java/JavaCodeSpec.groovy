package com.athaydes.osgiaas.cli.java

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Unroll
class JavaCodeSpec extends Specification {

    def "Can join Java lines correctly"() {
        given: 'Java Code'
        @Subject
        def code = new JavaCode()

        when: 'Example lines are added to the code'
        lines.each { code.addLine( it ) }

        and: 'the executableCode is requested'
        def result = code.executableCode

        then: 'the result is as expected'
        result == expectedResult

        where:
        lines                           | expectedResult
        [ ]                             | 'return null;'
        [ 'a' ]                         | 'a;\nreturn null;'
        [ 'a', 'b', 'c' ]               | 'a;\nb;\nc;\nreturn null;'
        [ 'return i + 2' ]              | 'return i + 2;\n'
        [ 'int i = 0', 'return i + 2' ] | 'int i = 0;\nreturn i + 2;\n'
    }

}
