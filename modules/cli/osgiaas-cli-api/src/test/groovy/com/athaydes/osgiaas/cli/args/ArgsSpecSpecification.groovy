package com.athaydes.osgiaas.cli.args

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ArgsSpecSpecification extends Specification {

    def "It should be very easy to specify command arguments"() {
        given: 'An ArgsSpec is built with a few simple options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).mandatory().withArgs( "file" ).end()
                .accepts( '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.options == expectedArgs
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command                      | expectedArgs                                  | expectedUnprocessedInput
        'x -f a'                     | [ '-f': [ [ 'a' ] ] ]                         | ''
        'x -f abc --no-fuss'         | [ '-f': [ [ 'abc' ] ], '--no-fuss': [ [ ] ] ] | ''
        'x -f abc something'         | [ '-f': [ [ 'abc' ] ] ]                       | 'something'
        'x --no-fuss -f a and stuff' | [ '-f': [ [ 'a' ] ], '--no-fuss': [ [ ] ] ]   | 'and stuff'
    }

    def "It should be easy to specify long form command arguments"() {
        given: 'An ArgsSpec is built with a few simple options and long options'
        def spec = ArgsSpec.builder()
                .accepts( '-f', '--fuss' ).mandatory().withArgs( "file" ).end()
                .accepts( '-n', '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.options == expectedArgs
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command                          | expectedArgs                           | expectedUnprocessedInput
        'x -f a'                         | [ '-f': [ [ 'a' ] ] ]                  | ''
        'x --fuss a'                     | [ '-f': [ [ 'a' ] ] ]                  | ''
        'x -f abc --no-fuss'             | [ '-f': [ [ 'abc' ] ], '-n': [ [ ] ] ] | ''
        'x -f abc -n'                    | [ '-f': [ [ 'abc' ] ], '-n': [ [ ] ] ] | ''
        'x -f abc something'             | [ '-f': [ [ 'abc' ] ] ]                | 'something'
        'x --fuss abc something'         | [ '-f': [ [ 'abc' ] ] ]                | 'something'
        'x --no-fuss --fuss a and stuff' | [ '-f': [ [ 'a' ] ], '-n': [ [ ] ] ]   | 'and stuff'
    }

    def "Command options may take multiple arguments"() {
        given: 'An ArgsSpec is built with options that take multiple options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).allowMultiple().withArgs( "a" ).withOptionalArgs( "b", "c" ).end()
                .accepts( '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the result args Map contains the options specified by the command invocation'
        result?.options == expectedArgs
        result.unprocessedInput == expectedUnprocessedInput

        where:
        command                               | expectedArgs                                                | expectedUnprocessedInput
        'x -f a -f b'                         | [ '-f': [ [ 'a' ], [ 'b' ] ] ]                              | ''
        'x -f abc --no-fuss -f d'             | [ '-f': [ [ 'abc' ], [ 'd' ] ], '--no-fuss': [ [ ] ] ]      | ''
        'x -f abc something'                  | [ '-f': [ [ 'abc', 'something' ] ] ]                        | ''
        'x  -f a and stuff'                   | [ '-f': [ [ 'a', 'and', 'stuff' ] ] ]                       | ''
        'x  -f a and stuff b'                 | [ '-f': [ [ 'a', 'and', 'stuff' ] ] ]                       | 'b'
        'x  -f a and stuff --no-fuss zz -f x' | [ '-f': [ [ 'a', 'and', 'stuff' ] ], '--no-fuss': [ [ ] ] ] | 'zz -f x'
        'x -f a b --no-fuss c'                | [ '-f': [ [ 'a', 'b' ] ], '--no-fuss': [ [ ] ] ]            | 'c'
        'x -f a b -f c d e f'                 | [ '-f': [ [ 'a', 'b' ], [ 'c', 'd', 'e' ] ] ]               | 'f'
    }

    def "Missing mandatory argument should cause an error"() {
        given: 'An ArgsSpec is built with a few options, including mandatory options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).mandatory().withArgs( "a" ).withOptionalArgs( "b", "c" ).end()
                .accepts( 'a' ).mandatory().end()
                .accepts( '-n', '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        spec.parse( command )

        then: 'an error is thrown with an appropriate message'
        def error = thrown( IllegalArgumentException )
        !error.message?.empty
        error && expectedErrors.every { error.message.contains( it ) }

        where:
        command              | expectedErrors
        'cmd'                | [ 'a', '-f' ]
        'cmd a'              | [ '-f' ]
        'cmd --no-fuss'      | [ 'a', '-f' ]
        'cmd -n'             | [ 'a', '-f' ]
        'cmd -f a'           | [ 'a' ]
        'cmd -f b --no-fuss' | [ 'a' ]
        'cmd -f b -n'        | [ 'a' ]
        'cmd -f b c'         | [ 'a' ]
    }

    def "Missing argument to parameter causes an error"() {
        given: 'An ArgsSpec is built with a few options, including mandatory argument options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).mandatory().withArgs( "a" ).withOptionalArgs( "b", "c" ).end()
                .accepts( '-a', '--aaa' ).withArgs( "a" ).end()
                .accepts( '--no-fuss' ).end()
                .build()

        when: 'some example command invocations are parsed'
        spec.parse( command )

        then: 'an error is thrown with an appropriate message'
        def error = thrown( IllegalArgumentException )
        !error.message?.empty
        error.message.contains( expectedError )

        where:
        command                 | expectedError
        'cmd -f'                | '-f'
        'cmd -f 1 -a'           | '-a'
        'cmd -f 1 --aaa'        | '-a'
        'cmd -f 1 --no-fuss -a' | '-a'
    }

    def "Command itself must always be removed from the results"() {
        given: 'An ArgsSpec with no mandatory options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).withArgs( "file" ).end()
                .build()

        when: 'some example command invocations are parsed'
        def result = spec.parse( command )

        then: 'the results never include the command itself'
        result.unprocessedInput == expectedUnprocessedInput
        result.options == expectedArgs

        where:
        command            | expectedArgs          | expectedUnprocessedInput
        'hello'            | [ : ]                 | ''
        'hello hi'         | [ : ]                 | 'hi'
        'hello -f a'       | [ '-f': [ [ 'a' ] ] ] | ''
        'hello -f a b'     | [ '-f': [ [ 'a' ] ] ] | 'b'
        'hello -f a b cde' | [ '-f': [ [ 'a' ] ] ] | 'b cde'
    }

    def "Usage for extremely simple command works as expected"() {
        given: 'An extremely simple ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( 'a' ).mandatory().end()
                .build()

        when: 'we request usage for the ArgsSpec'
        def result = spec.usage

        then: 'the usage is as expected'
        result == 'a'
    }

    def "Documentation for extremely simple command works as expected"() {
        given: 'An extremely simple ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( 'a' ).mandatory().end()
                .build()

        when: 'we request documentation for the ArgsSpec'
        def result = spec.documentation

        then: 'the documentation is as expected'
        result == '* a'
    }

    def "Usage for simple command works as expected"() {
        given: 'A simple ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( 'a' ).end()
                .accepts( 'bc' ).withArgs( 'hi' ).withDescription( 'this is description' ).end()
                .build()

        when: 'we request usage for the ArgsSpec'
        def result = spec.usage

        then: 'the usage is as expected'
        result == '[a] [bc <hi>]'
    }

    def "Documentation for simple command works as expected"() {
        given: 'A simple ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( 'a' ).end()
                .accepts( 'bc' ).withArgs( 'hi' ).withDescription( 'this is description' ).end()
                .build()

        when: 'we request documentation for the ArgsSpec'
        def result = spec.documentation

        then: 'the documentation is as expected'
        result == """|* [a]
            |* [bc] <hi>
            |this is description""".stripMargin()
    }

    def "Usage for complex command works as expected"() {
        given: 'A complex ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( '-a' ).allowMultiple().end()
                .accepts( '-z', '--zzz' ).mandatory().allowMultiple().withArgs( "hi" ).end()
                .accepts( 'bc' ).withArgs( 'hi' ).withDescription( 'this is description' ).end()
                .accepts( '-f', '--file' ).mandatory().withArgs( 'hi', 'bye' )
                .withOptionalArgs( 'ho', 'ho' ).withDescription( 'very complex one' ).end()
                .build()

        when: 'we request usage for the ArgsSpec'
        def result = spec.usage

        then: 'the usage is as expected'
        result == "[-a…] -z… <hi> [bc <hi>] -f <hi> <bye> [<ho> <ho>]"
    }

    def "Documentation for complex command works as expected"() {
        given: 'A complex ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( '-a' ).allowMultiple().end()
                .accepts( '-z', '--zzz' ).mandatory().allowMultiple().withArgs( "hi" ).end()
                .accepts( 'bc' ).withArgs( 'hi' ).withDescription( 'this is description' ).end()
                .accepts( '-f', '--file' ).mandatory().withArgs( 'hi', 'bye' )
                .withOptionalArgs( 'ho', 'ho' ).withDescription( 'very complex one' ).end()
                .build()

        when: 'we request documentation for the ArgsSpec'
        def result = spec.documentation

        then: 'the documentation is as expected'
        result == """|* [-a…]
            |* -z…, --zzz… <hi>
            |* [bc] <hi>
            |this is description
            |* -f, --file <hi> <bye> [<ho> <ho>]
            |very complex one""".stripMargin()
    }

    def "Documentation for complex command with indentation works as expected"() {
        given: 'A complex ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( '-a' ).allowMultiple().end()
                .accepts( '-z', '--zzz' ).mandatory().allowMultiple().withArgs( "hi" ).end()
                .accepts( 'bc' ).withArgs( 'hi' ).withDescription( 'this is description' ).end()
                .accepts( '-f', '--file' ).mandatory().withArgs( 'hi', 'bye' )
                .withOptionalArgs( 'ho', 'ho' ).withDescription( 'very complex one' ).end()
                .build()

        when: 'we request documentation for the ArgsSpec'
        def result = spec.getDocumentation( '  ' )

        then: 'the documentation is as expected'
        result == """|  * [-a…]
            |  * -z…, --zzz… <hi>
            |  * [bc] <hi>
            |    this is description
            |  * -f, --file <hi> <bye> [<ho> <ho>]
            |    very complex one""".stripMargin()
    }

    def "Completer can be used to auto-complete a simple command"() {
        given: 'An ArgsSpec is built with a few simple options'
        def spec = ArgsSpec.builder()
                .accepts( '-f' ).mandatory().withArgs( "file" ).end()
                .accepts( '--no-fuss' ).end()
                .build()

        when: 'The completer generated by the ArgsSpec is used to complete a command'
        def candidates = [ ]
        def completer = spec.getCommandCompleter( 'hi' )
        def completionIndex = completer.complete( command, command.size(), candidates )

        then: 'The expected command completions are returned'
        candidates == expectedCompletions

        and: 'The completion index is as expected'
        completionIndex == expectedIndex

        where:
        command           | expectedIndex | expectedCompletions
        'hi '             | 3             | [ '--no-fuss', '-f' ]
        'hi -'            | 3             | [ '--no-fuss', '-f' ]
        'hi --'           | 3             | [ '--no-fuss' ]
        'hi --no-'        | 3             | [ '--no-fuss' ]
        'hi --no-fuss -'  | 13            | [ '-f' ]
        'hi -f '          | 6             | [ '--no-fuss' ]
        'hi -f -'         | 6             | [ '--no-fuss' ]
        'hi -f'           | 3             | [ '-f' ]
        'hi --no-fuss --' | -1            | [ ]
        'hi h'            | -1            | [ ]
        'hi -f file -g '  | -1            | [ ]
        'ho '             | -1            | [ ]
        'ho -'            | -1            | [ ]
    }

    def "Completer can be used to auto-complete a complex command"() {
        given: 'A complex ArgsSpec'
        def spec = ArgsSpec.builder()
                .accepts( '-a' ).allowMultiple().end()
                .accepts( '-z', '--zzz' ).mandatory().allowMultiple().withArgs( "hi" ).end()
                .accepts( 'bc' ).withArgs( 'hi' ).withDescription( 'this is description' ).end()
                .accepts( '-f', '--file' ).mandatory().withArgs( 'hi', 'bye' ).withOptionalArgs( 'ok' )
                .withOptionalArgs( 'ho', 'ho' ).withDescription( 'very complex one' ).end()
                .build()

        when: 'The completer generated by the ArgsSpec is used to complete a command'
        def candidates = [ ]
        def completer = spec.getCommandCompleter( 'hi' )
        def completionIndex = completer.complete( command, command.size(), candidates )

        then: 'The expected command completions are returned'
        candidates == expectedCompletions

        and: 'The completion index is as expected'
        completionIndex == expectedIndex

        where:
        command          | expectedIndex | expectedCompletions
        'hi '            | 3             | [ '--file', '--zzz', '-a', '-f', '-z', 'bc' ]
        'hi --'          | 3             | [ '--file', '--zzz' ]
        'hi -a '         | 6             | [ '--file', '--zzz', '-a', '-f', '-z', 'bc' ]
        'hi -a  '        | 7             | [ '--file', '--zzz', '-a', '-f', '-z', 'bc' ]
        'hi -a -z '      | 9             | [ '--file', '--zzz', '-a', '-f', '-z', 'bc' ]
        'hi -f a b '     | 10            | [ '--zzz', '-a', '-z', 'bc' ]
        'hi -f a b -z '  | 13            | [ '--zzz', '-a', '-z', 'bc' ]
        'hi -f a b c '   | 12            | [ '--zzz', '-a', '-z', 'bc' ]
        'hi -f a b c -z' | 12            | [ '-z' ]
        'hi xx -'        | -1            | [ ]
    }

}
