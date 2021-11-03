plugins {
    id("org.asciidoctor.jvm.convert")
    id("org.ajoberstar.git-publish")
}

tasks {
    asciidoctor {
        baseDirFollowsSourceDir()
        resources {
            from("src/docs/asciidoc/highlight") {
                into("highlight")
            }
            from("src/docs/asciidoc/css") {
                into("css")
            }
            from("src/docs/asciidoc/js") {
                into("js")
            }
        }
    }
}

gitPublish {

    branch.set("gh-pages")
    sign.set(false)

    contents {
        from(tasks.asciidoctor) {
            into(providers.provider { "$version" })
        }
    }

    preserve {
        include("**")
        exclude(KotlinClosure1<FileVisitDetails, Boolean>({ name.equals("$version") }, this, this))
    }

    commitMessage.set(providers.provider {
        "Publishing documentation for version $version"
    })
}
