plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform.module)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("intellij.version"))
        // Gradle module model of the IDE
        bundledPlugins("com.intellij.gradle")
    }

    implementation(projects.components.core.intellij)
    implementation(projects.components.host.api)
}
