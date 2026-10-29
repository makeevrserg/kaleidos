plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
    }

    implementation(projects.components.core.api)
    implementation(projects.components.core.intellij)
    implementation(projects.components.harness.api)
    implementation(projects.components.host.intellij)
    implementation(projects.components.preview.api)
    implementation(projects.components.psi)
}
