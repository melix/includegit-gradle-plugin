plugins {
    `java-gradle-plugin`
    `java-test-fixtures`
    groovy
    id("com.gradle.plugin-publish") version "0.16.0"
    `maven-publish`
    signing
    id("me.champeau.internal.build.functional-testing")
}

group = "me.champeau.gradle.includegit"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation(libs.bundles.jgit)

    testFixturesApi(libs.groovy)
    testFixturesApi(libs.spock)
    testFixturesImplementation(gradleTestKit())
}

publishing {
    repositories {
        maven {
            name = "build"
            url  = uri("${buildDir}/repo")
        }
    }
}

signing {
    useGpgCmd()
    publishing.publications.configureEach {
        sign(this)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}


gradlePlugin {
    val includeBuildPlugin by plugins.creating {
        id = "me.champeau.includegit"
        implementationClass = "me.champeau.gradle.igp.IncludeGitPlugin"
    }
}

afterEvaluate {
    pluginBundle {
        website = "https://melix.github.io/includegit-gradle-plugin/"
        vcsUrl = "https://github.com/melix/includegit-gradle-plugin"
        description = "Adds support for including Git repositories"
        tags = listOf("git", "included builds")

        plugins {
            named("includeBuildPlugin") {
                displayName = "Gradle Include Git repositories plugin"
            }
        }
        mavenCoordinates {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
        }
    }
}
