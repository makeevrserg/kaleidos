pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
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
