import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    `java-test-fixtures`
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.CORE_API)
    rootPackage.set("com.makeevrserg.kaleidos.core")
}

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlin.stdlib)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
