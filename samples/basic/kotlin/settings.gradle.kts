plugins {
    id("me.champeau.includegit")
}

rootProject.name = "basic"


configure<me.champeau.gradle.igp.GitIncludeExtension> {
    include("testlib0") {
        uri.set("https://github.com/melix/includegit-gradle-plugin.git")
        branch.set("testlib-0")
    }
}
