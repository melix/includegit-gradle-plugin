plugins {
    id("me.champeau.includegit")
}

rootProject.name = "basic"

gitRepositories {
    include("openbeans") {
        uri.set("https://github.com/melix/openbeans.git")
        branch.set("master")
    }
}
