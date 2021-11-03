import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

plugins {
    `java-gradle-plugin`
}

val functionalTestSourceSet = sourceSets.create("functionalTest")

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
val supportedGradleVersions: Set<String> by extra

val groovyDslFunctionalTests by tasks.registering
val kotlinDslFunctionalTests by tasks.registering

supportedGradleVersions.forEach { gradleVersion ->
    listOf("groovy", "kotlin").forEach { dsl ->
        val taskName = if (gradleVersion == "") {
            if (dsl == "groovy") {
                "functionalTest"
            } else {
                "kotlinDslFunctionalTest"
            }
        } else {
            val suffix = if (dsl == "groovy") {
                "FunctionalTest"
            } else {
                "kotlinDslFunctionalTest"
            }
            "gradle${gradleVersion.replace('.', '_')}$suffix"
        }

        val functionalTest = tasks.register<Test>(taskName) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Functional tests using Gradle $gradleVersion and $dsl DSL"
            inputs.files("../samples")
            testClassesDirs = functionalTestSourceSet.output.classesDirs
            classpath = functionalTestSourceSet.runtimeClasspath
            systemProperty("gradleVersion", gradleVersion)
            systemProperty("dsl", dsl)
        }

        tasks.check {
            dependsOn(functionalTest)
        }

        if (dsl == "groovy") {
            groovyDslFunctionalTests.configure { dependsOn(functionalTest) }
        } else {
            kotlinDslFunctionalTests.configure { dependsOn(functionalTest) }
        }
    }
}
