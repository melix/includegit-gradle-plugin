pluginManagement {
    includeBuild("build-logic") {
        name = "includegit-buildlogic"
    }
}

rootProject.name = "gradle-includegit-plugin"

include("docs")
include("plugin")

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

gradle.beforeProject {
    extra.set("supportedGradleVersions", setOf(
            "", // current,
            "6.2",
            "6.3",
            "6.4",
            "7.1.1",
            "7.2",
            "7.3-rc-3"
    ))
    extra.set("unsupportedGradleVersions", setOf(
            "6.5.x",
            "6.6.x",
            "6.7.x",
            "6.8.x",
            "6.9.x",
            "7.0.x",
    ))
}
