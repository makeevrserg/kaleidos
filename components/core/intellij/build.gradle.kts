plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
    }

    implementation(projects.components.core.api)
}
