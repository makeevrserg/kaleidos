import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.PLATFORM)
    rootPackage.set("com.makeevrserg.kaleidos.server.intellij")
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
