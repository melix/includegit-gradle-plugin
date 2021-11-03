import me.champeau.gradle.igp.gitRepositories

plugins {
    id("me.champeau.includegit")
}

rootProject.name = "basic"


gitRepositories {
    include("testlib0") {
        uri.set("https://github.com/melix/includegit-gradle-plugin.git")
        branch.set("testlib-0")
    }
}
