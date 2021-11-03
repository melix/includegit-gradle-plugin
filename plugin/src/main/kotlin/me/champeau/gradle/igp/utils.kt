@file:Suppress("unused")

package me.champeau.gradle.igp

import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.configure

fun Settings.gitRepositories(repos: GitIncludeExtension.() -> Unit) = configure(repos)
