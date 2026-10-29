plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
        // Gradle task execution through the external system API
        bundledPlugins("com.intellij.gradle")
    }

    implementation(projects.components.core.api)
    implementation(projects.components.core.intellij)
    implementation(projects.components.server.api)
}
