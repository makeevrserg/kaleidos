import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "compose-html-preview"

include(":plugin")
// Components. `api` modules are plain Kotlin without the IntelliJ Platform; `intellij` modules hold the adapters.
include(
    ":components:core:api",
    ":components:core:intellij",
)
include(
    ":components:host:api",
    ":components:host:intellij",
)
include(
    ":components:server:api",
    ":components:server:intellij",
)
include(
    ":components:preview:api",
    ":components:preview:intellij",
)
include(":components:psi")
include(":components:ui")
