import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    `java-test-fixtures`
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.API)
    rootPackage.set("com.makeevrserg.kaleidos.harness.api")
}

dependencies {
    testFixturesApi(projects.components.host.api)
    testFixturesImplementation(libs.kotlin.stdlib)

    // Provided by the IntelliJ Platform at runtime
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)

    implementation(projects.components.host.api)
    implementation(projects.components.core.api)

    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.tests.kotlin.test)
}
