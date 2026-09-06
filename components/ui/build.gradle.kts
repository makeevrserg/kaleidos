plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
        // JCEF left the platform core in 2026.2 and lives in the "Web Browser (JCEF)" plugin
        bundledModules("intellij.platform.ui.jcef", "intellij.libraries.jcef")
    }

    implementation(projects.components.core.api)
    implementation(projects.components.preview.api)
}
