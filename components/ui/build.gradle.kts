plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("intellij.version"))
    }

    implementation(projects.components.core.api)
    implementation(projects.components.preview.api)
}
