package me.champeau.gradle.igp

import me.champeau.includegit.AbstractFunctionalTest

class BasicFunctionalTest extends AbstractFunctionalTest {
    def "can include a remote git repository"() {
        withSample 'basic'

        when:
        run 'dependencies', '--configuration', 'compileClasspath'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib0
'''
    }

    def "can use a local clone"() {
        withSample 'basic'

        when:
        fails 'dependencies', '--configuration', 'compileClasspath', '-Dlocal.git.testlib0=/xxx'

        then:
        errorOutputContains '''Included build '/xxx' does not exist'''
    }

}
