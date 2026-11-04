import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    `java-test-fixtures`
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.API)
    rootPackage.set("com.makeevrserg.kaleidos.host.api")
}

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlin.stdlib)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.tests.kotlin.test)
}
