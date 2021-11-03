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
            "6.8",
            "7.1.1",
            "7.2",
            "7.3-rc-3"
    ))
}
