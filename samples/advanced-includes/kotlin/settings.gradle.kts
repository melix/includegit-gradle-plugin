import me.champeau.gradle.igp.gitRepositories

plugins {
    id("me.champeau.includegit")
}

rootProject.name = "basic"


gitRepositories {
    if (System.getProperty("autoInclude") != null) {
        include("testlib0") {
            uri.set("https://github.com/melix/includegit-gradle-plugin.git")
            branch.set("testlib-0")
            autoInclude.set(java.lang.Boolean.getBoolean("autoInclude"))
        }
    } else if (System.getProperty("subdir") != null) {
        include("testlib1") {
            uri.set("https://github.com/melix/includegit-gradle-plugin.git")
            branch.set("testlib-1")
            includeBuild(System.getProperty("subdir")) {
                name = "testlib1"
            }
        }
    }
}
