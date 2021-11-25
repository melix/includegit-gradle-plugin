package me.champeau.gradle.igp

import me.champeau.includegit.AbstractFunctionalTest

class AdvancedIncludesFunctionalTest extends AbstractFunctionalTest {

    def "can disable auto-include of a remote git repository"() {
        withSample 'advanced-includes'

        when:
        run 'dependencies', '--configuration', 'compileClasspath', '-DautoInclude=false'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 FAILED
'''

    }

    def "can include a specific subdirectory of a checkout"() {
        withSample 'advanced-includes'

        when:
        run 'dependencies', '--configuration', 'compileClasspath', "-Dsubdir=$dirName"

        then:
        tasks {
            succeeded ':dependencies'
        }
        file("checkouts/testlib1/$dirName").directory
        file("checkouts/testlib1/$dirName/build.gradle").exists()

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
\\--- com.acme.somelib:somelib1:0.0 -> project :testlib1
     \\--- org.apache.commons:commons-math3:3.6.1 FAILED
'''

        where:
        dirName << ["sub1", "sub2"]
    }

}
