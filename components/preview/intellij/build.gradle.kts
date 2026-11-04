import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.PLATFORM)
    rootPackage.set("com.makeevrserg.kaleidos.preview.intellij")
}

dependencies {
    implementation(projects.components.core.api)
    implementation(projects.components.core.intellij)
    implementation(projects.components.harness.api)
    implementation(projects.components.host.api)
    implementation(projects.components.preview.api)
    implementation(projects.components.server.api)
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
    }
}
