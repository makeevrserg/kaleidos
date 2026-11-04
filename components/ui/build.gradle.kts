import ru.astrainteractive.klibs.rules.core.unit.model.UnitRole

plugins {
    id("kaleidos.kotlin.jvm")
    alias(libs.plugins.kotlin.compose)
    id("kaleidos.intellij.module")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unit(UnitRole.Platform())
    rootPackage.set("com.makeevrserg.kaleidos.ui")
}

dependencies {
    compileOnly(libs.compose.ui.tooling.preview) { isTransitive = false }
    implementation(projects.components.core.api)
    implementation(projects.components.harness.api)
    implementation(projects.components.host.api)
    implementation(projects.components.preview.api)
    implementation(projects.components.server.api)
    intellijPlatform {
        bundledModules(
            "intellij.libraries.compose.foundation.desktop",
            "intellij.libraries.compose.runtime.desktop",
            "intellij.libraries.jcef",
            "intellij.platform.jewel.foundation",
            "intellij.platform.jewel.ideLafBridge",
            "intellij.platform.jewel.ui",
            "intellij.platform.ui.jcef"
        )
        intellijIdea(providers.gradleProperty("intellij.version"))
    }
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.tests.kotlin.test)
}
