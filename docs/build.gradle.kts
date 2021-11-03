plugins {
    id("me.champeau.internal.build.documentation")
}

val supportedGradleVersions: Set<String> by extra
val unsupportedGradleVersions: Set<String> by extra

fun toVersionList(versions: Set<String>) = versions.map {
    if ("" == it) {
        GradleVersion.current().version
    } else {
        it
    }
}.toSortedSet().joinToString(", ")

tasks {
    asciidoctor {
        val testedVersions = toVersionList(supportedGradleVersions)
        val brokenVersions = toVersionList(unsupportedGradleVersions)
        attributes(mapOf(
                "reproducible" to "",
                "nofooter" to "",
                "toc" to "left",
                "docinfo" to "shared",
                "source-highlighter" to "highlight.js",
                "highlightjs-theme" to "equilibrium-light",
                "highlightjsdir" to "highlight",
                "tested-versions" to testedVersions,
                "broken-versions" to brokenVersions
        ))
    }
}

gitPublish {
    repoUri.set("git@github.com:melix/includegit-gradle-plugin.git")
}
