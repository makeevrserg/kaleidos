import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.API)
    rootPackage.set("com.makeevrserg.kaleidos.preview.api")
}

dependencies {
    // Provided by the IntelliJ Platform at runtime
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)

    implementation(projects.components.host.api)
    implementation(projects.components.server.api)
    implementation(projects.components.harness.api)
    implementation(projects.components.core.api)

    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.tests.kotlin.test)
    testImplementation(testFixtures(projects.components.core.api))
    testImplementation(testFixtures(projects.components.harness.api))
    testImplementation(testFixtures(projects.components.host.api))
    testImplementation(testFixtures(projects.components.server.api))
}
