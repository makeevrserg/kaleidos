import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.PLATFORM)
    rootPackage.set("com.makeevrserg.kaleidos.host.intellij")
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
