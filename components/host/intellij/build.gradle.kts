plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
        // Gradle module model of the IDE
        bundledPlugins("com.intellij.gradle")
    }

    implementation(projects.components.core.api)
    implementation(projects.components.core.intellij)
    implementation(projects.components.host.api)
}
