package me.champeau.gradle.igp

import me.champeau.includegit.AbstractFunctionalTest
import spock.lang.Issue

class BasicFunctionalTest extends AbstractFunctionalTest {

    def "can include a remote git repository"() {
        withSample 'basic'

        when:
        run 'dependencies', '--configuration', 'compileClasspath', useCommit?'-PuseCommit=true':'-Pdummy'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib0
'''

        where:
        useCommit << [false, true]
    }

    def "can switch from commit to branch"() {
        withSample 'basic'

        when:
        run 'dependencies', '--configuration', 'compileClasspath', '-PuseCommit=true'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains 'Using commit'
        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib0
'''

        when:
        run 'dependencies', '--configuration', 'compileClasspath'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains 'Using branch'
        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib0
'''
    }

    def "can switch from branch to commit"() {
        withSample 'basic'

        when:
        run 'dependencies', '--configuration', 'compileClasspath'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains 'Using branch'
        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib0
'''

        when:
        run 'dependencies', '--configuration', 'compileClasspath', '-PuseCommit=true'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains 'Using commit'
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

    def "can automatically use local repository instead of checking out"() {
        withSample 'basic'

        file("gradle.properties") << """
auto.include.git.dirs=${new File("../samples/repo").absolutePath}
        """

        when:
        run 'dependencies', '--configuration', 'compileClasspath'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib0
     +--- org.apache.commons:commons-math3:3.6.1 FAILED
     \\--- dummy:for-test:1.0 FAILED
'''
    }

    @Issue("https://github.com/melix/includegit-gradle-plugin/issues/10")
    def "can use a specific directory for checkout"() {
        withSample 'basic'
        debug = true

        when:
        run 'dependencies', '--configuration', 'compileClasspath', '-PcheckoutDir=here'

        then:
        tasks {
            succeeded ':dependencies'
        }

        file("here").directory
        file("here/build.gradle").exists()

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib0
'''
    }
}
