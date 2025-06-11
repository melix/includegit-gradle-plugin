pluginManagement {
    includeBuild("build-logic") {
        name = "includegit-buildlogic"
    }
}

rootProject.name = "gradle-includegit-plugin"

include("docs")
include("plugin")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

gradle.beforeProject {
    extra.set("supportedGradleVersions", setOf(
            "", // current,
            // "7.1.1",
            "7.2",
            "7.3",
            "8.14.2",
    ))
    extra.set("unsupportedGradleVersions", setOf(
            "6.0.x",
            "6.1.x"
    ))
}
