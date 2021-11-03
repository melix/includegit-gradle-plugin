package me.champeau.includegit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GFileUtils
import org.gradle.util.GradleVersion
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class AbstractFunctionalTest extends Specification {
    private final String gradleVersion = System.getProperty("gradleVersion", GradleVersion.current().version)
    private final String dsl = System.getProperty("dsl", "groovy")

    @TempDir
    Path testDirectory

    boolean debug

    private StringWriter outputWriter
    private StringWriter errorOutputWriter
    private String output
    private String errorOutput

    BuildResult result

    Path path(String... pathElements) {
        Path cur = testDirectory
        pathElements.each {
            cur = cur.resolve(it)
        }
        cur
    }

    File file(String... pathElements) {
        path(pathElements).toFile()
    }

    File getGroovyBuildFile() {
        file("build.gradle")
    }

    File getKotlinBuildFile() {
        file("build.gradle.kts")
    }

    File getGroovySettingsFile() {
        file("settings.gradle")
    }

    File getKotlinSettingsFile() {
        file("settings.gradle.kts")
    }

    File getBuildFile() {
        groovyBuildFile
    }

    File getSettingsFile() {
        groovySettingsFile
    }

    protected void withSample(String name) {
        File sampleDir = new File("../samples/$name/$dsl")
        GFileUtils.copyDirectory(sampleDir, testDirectory.toFile())
        def patchFile = "groovy" == dsl ? buildFile : kotlinBuildFile
        patchFile.text = patchFile.text.replace(' version "{gradle-project-version}"', '')
    }

    void run(String... args) {
        try {
            result = newRunner(args)
                    .build()
        } finally {
            recordOutputs()
        }
    }

    void outputContains(String text) {
        assert output.contains(text)
    }

    void outputDoesNotContain(String text) {
        assert !output.contains(text)
    }

    void errorOutputContains(String text) {
        assert errorOutput.contains(text)
    }

    void tasks(@DelegatesTo(value = TaskExecutionGraph, strategy = Closure.DELEGATE_FIRST) Closure spec) {
        def graph = new TaskExecutionGraph()
        spec.delegate = graph
        spec.resolveStrategy = Closure.DELEGATE_FIRST
        spec()
    }

    private void recordOutputs() {
        output = outputWriter.toString()
        errorOutput = errorOutputWriter.toString()
    }

    private GradleRunner newRunner(String... args) {
        outputWriter = new StringWriter()
        errorOutputWriter = new StringWriter()
        ArrayList<String> autoArgs = computeAutoArgs()
        def runner = GradleRunner.create()
                .forwardStdOutput(tee(new OutputStreamWriter(System.out), outputWriter))
                .forwardStdError(tee(new OutputStreamWriter(System.err), errorOutputWriter))
                .withPluginClasspath()
                .withProjectDir(testDirectory.toFile())
                .withArguments([*autoArgs, *args])
        if (gradleVersion) {
            runner.withGradleVersion(gradleVersion)
        }
        if (debug) {
            runner.withDebug(true)
        }
        runner
    }

    private ArrayList<String> computeAutoArgs() {
        List<String> autoArgs = [
                "-s",
        ]
        if (Boolean.getBoolean("config.cache")) {
            autoArgs << '--configuration-cache'
        }
        autoArgs
    }

    private static Writer tee(Writer one, Writer two) {
        return TeeWriter.of(one, two)
    }

    void fails(String... args) {
        try {
            result = newRunner(args)
                    .buildAndFail()
        } finally {
            recordOutputs()
        }
    }

    private class TaskExecutionGraph {
        void succeeded(String... tasks) {
            tasks.each { task ->
                contains(task)
                assert result.task(task).outcome == TaskOutcome.SUCCESS
            }
        }

        void failed(String... tasks) {
            tasks.each { task ->
                contains(task)
                assert result.task(task).outcome == TaskOutcome.FAILED
            }
        }

        void skipped(String... tasks) {
            tasks.each { task ->
                contains(task)
                assert result.task(task).outcome == TaskOutcome.SKIPPED
            }
        }

        void contains(String... tasks) {
            tasks.each { task ->
                assert result.task(task) != null: "Expected to find task $task in the graph but it was missing"
            }
        }

        void doesNotContain(String... tasks) {
            tasks.each { task ->
                assert result.task(task) == null: "Task $task should be missing from the task graph but it was found with an outcome of ${result.task(task).outcome}"
            }
        }
    }
}
