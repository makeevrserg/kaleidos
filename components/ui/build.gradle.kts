plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
        // JCEF left the platform core in 2026.2 and lives in the "Web Browser (JCEF)" plugin
        bundledModules("intellij.platform.ui.jcef", "intellij.libraries.jcef")
        // Compose Multiplatform and Jewel ship with the IDE. They must stay platform modules and never
        // be bundled with the plugin: skiko loads its native library once per JVM, so a second copy of
        // Compose in the plugin classloader breaks both of them.
        bundledModules(
            "intellij.libraries.compose.runtime.desktop",
            "intellij.libraries.compose.foundation.desktop",
            "intellij.platform.jewel.foundation",
            "intellij.platform.jewel.ui",
            "intellij.platform.jewel.ideLafBridge"
        )
    }

    // @Preview annotations only, taken without transitives so no second Compose reaches the compile
    // classpath. Never at runtime: the IDE reads the annotations, the composables never touch them.
    compileOnly(libs.compose.ui.tooling.preview) { isTransitive = false }

    implementation(projects.components.core.api)
    implementation(projects.components.preview.api)

    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.tests.kotlin.test)
}
