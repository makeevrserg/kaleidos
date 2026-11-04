import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    `java-test-fixtures`
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.API)
    rootPackage.set("com.makeevrserg.kaleidos.server.api")
}

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)
    implementation(projects.components.core.api)
    implementation(projects.components.host.api)
    testFixturesApi(libs.kotlinx.coroutines.core)
    testFixturesApi(projects.components.host.api)
    testFixturesImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.tests.kotlin.test)
    testImplementation(testFixtures(projects.components.core.api))
}
