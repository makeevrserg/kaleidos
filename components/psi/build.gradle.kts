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
        // Kotlin PSI; the Kotlin plugin itself depends on the Java plugin
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")
    }

    implementation(projects.components.preview.api)
}
