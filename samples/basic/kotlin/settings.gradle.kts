import me.champeau.gradle.igp.gitRepositories

plugins {
    id("me.champeau.includegit")
}

rootProject.name = "basic"


gitRepositories {
    include("testlib0") {
        uri.set("https://github.com/melix/includegit-gradle-plugin.git")
        if (gradle.startParameter.projectProperties.containsKey("useCommit")) {
            println("Using commit")
            commit.set("df01b5ffd")
        } else {
            println("Using branch")
            branch.set("testlib-0")
        }
        if (gradle.startParameter.projectProperties.containsKey("checkoutDir")) {
            checkoutDirectory.set(file(gradle.startParameter.projectProperties.get("checkoutDir")!!))
        }
        codeReady {
            println("Code ready")
            println("Checkout directory: ${checkoutDirectory}")
        }
    }
}
