plugins {
    id("me.champeau.internal.build.documentation")
}

val supportedGradleVersions: Set<String> by extra

tasks {
    asciidoctor {
        val testedVersions = supportedGradleVersions.map {
            if ("" == it) {
                GradleVersion.current().version
            } else {
                it
            }
        }.toSortedSet().joinToString(", ")
        attributes(mapOf(
                "reproducible" to "",
                "nofooter" to "",
                "toc" to "left",
                "docinfo" to "shared",
                "source-highlighter" to "highlight.js",
                "highlightjs-theme" to "equilibrium-light",
                "highlightjsdir" to "highlight",
                "tested-versions" to testedVersions
        ))
    }
}

gitPublish {
    repoUri.set("git@github.com:melix/includegit-gradle-plugin.git")
}
