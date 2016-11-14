package com.athaydes.osgiaas.cli.java

import com.athaydes.osgiaas.cli.java.api.Binding
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Unroll
class JavaCodeSpec extends Specification {

    def "Can join Java lines correctly"() {
        given: 'Java Code'
        @Subject
        def code = new JavaCode( addBindingsToCode: false )

        when: 'Example lines are added to the code'
        lines.each { code.addLine( it ) }

        and: 'the executableCode is requested'
        def result = code.executableCode

        then: 'the result is as expected'
        result == expectedResult

        where:
        lines                           | expectedResult
        [ ]                             | 'return null;\n'
        [ 'a' ]                         | 'a;\nreturn null;\n'
        [ 'a', 'b', 'c' ]               | 'a;\nb;\nc;\nreturn null;\n'
        [ 'return i + 2' ]              | 'return i + 2;\n'
        [ 'int i = 0', 'return i + 2' ] | 'int i = 0;\nreturn i + 2;\n'
    }

    def "Should be able to turn all bindings into local variables"() {
        given: 'Java code containing some bindings'
        @Subject
        def code = new JavaCode( addBindingsToCode: true )

        Binding.binding.with {
            put 'one', 1 as int
            put 'hello', 'Hello World'
            put 'bool', true as boolean
            put 'obj', new Object()
            put 'list', [ 1, 2, 3 ]
        }

        when: 'the executableCode is requested'
        def result = code.executableCode

        then: 'the result is as expected'
        result == '''|int one = 1;
        |String hello = "Hello World";
        |boolean bool = true;
        |Object obj = binding.get("obj");
        |java.util.ArrayList list = binding.get("list");
        |PrintStream out = Binding.out;
        |PrintStream err = Binding.err;
        |BundleContext ctx = Binding.ctx;
        |Map<Object, Object> binding = Binding.binding;
        |return null;
        |'''.stripMargin()
    }

}
