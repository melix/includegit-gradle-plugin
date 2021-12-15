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
+--- com.acme.somelib:somelib1:0.0 FAILED
\\--- com.acme.somelib:somelib2 FAILED
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

        switch (dirName) {
            case 'sub1':
                outputContains '''compileClasspath - Compile classpath for source set 'main'.
+--- com.acme.somelib:somelib1:0.0 -> project :testlib1
|    \\--- org.apache.commons:commons-math3:3.6.1 FAILED
\\--- com.acme.somelib:somelib2 FAILED
'''
                break;

            case 'sub2':
                outputContains '''compileClasspath - Compile classpath for source set 'main'.
+--- com.acme.somelib:somelib1:0.0 FAILED
\\--- com.acme.somelib:somelib2 -> project :testlib1
     \\--- org.apache.commons:commons-math3:3.6.1 FAILED
'''
                break;
        }

        where:
        dirName << ["sub1", "sub2"]
    }

    def "can include multiple subdirectories of a checkout"() {
        withSample 'advanced-includes'

        when:
        run 'dependencies', '--configuration', 'compileClasspath', '-Dsubdir=sub1', '-Dsubdir2=sub2'

        then:
        tasks {
            succeeded ':dependencies'
        }
        file("checkouts/testlib1/sub1").directory
        file("checkouts/testlib1/sub1/build.gradle").exists()
        file("checkouts/testlib1/sub2").directory
        file("checkouts/testlib1/sub2/build.gradle").exists()

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
+--- com.acme.somelib:somelib1:0.0 -> project :testlib1
|    \\--- org.apache.commons:commons-math3:3.6.1 FAILED
\\--- com.acme.somelib:somelib2 -> project :testlib1_2
     \\--- org.apache.commons:commons-math3:3.6.1 FAILED
'''
    }

    def "can include subdirectories and root directory of a checkout"() {
        withSample 'advanced-includes'

        when:
        run 'dependencies', '--configuration', 'compileClasspath', '-Dsubdir=sub1', '-Dsubdir2='

        then:
        tasks {
            succeeded ':dependencies'
        }
        file("checkouts/testlib1/sub1").directory
        file("checkouts/testlib1/sub1/build.gradle").exists()
        file("checkouts/testlib1/sub2").directory
        file("checkouts/testlib1/sub2/build.gradle").exists()

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
+--- com.acme.somelib:somelib1:0.0 -> project :testlib1
|    \\--- org.apache.commons:commons-math3:3.6.1 FAILED
\\--- com.acme.somelib:somelib2 -> project :testlib
     \\--- org.apache.commons:commons-math3:3.6.1 FAILED
'''
    }

    def "can configure the main included build"() {
        withSample 'advanced-includes'

        when:
        run 'dependencies', '--configuration', 'compileClasspath', '-DmainIncludeName=hello'

        then:
        tasks {
            succeeded ':dependencies'
        }

        outputContains '''compileClasspath - Compile classpath for source set 'main'.
+--- com.acme.somelib:somelib1:0.0 -> project :hello
|    \\--- org.apache.commons:commons-math3:3.6.1 FAILED
\\--- com.acme.somelib:somelib2 FAILED
'''
    }

}
