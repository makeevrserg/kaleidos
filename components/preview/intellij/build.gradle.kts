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
    }

    implementation(projects.components.core.api)
    implementation(projects.components.core.intellij)
    implementation(projects.components.host.intellij)
    implementation(projects.components.preview.api)
    implementation(projects.components.psi)
}
