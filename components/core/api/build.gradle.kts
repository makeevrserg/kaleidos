plugins {
    id("kaleidos.kotlin.jvm")
    `java-test-fixtures`
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind.set("api")
    rootPackage.set("com.makeevrserg.kaleidos.core")
}

dependencies {
    // Provided by the IntelliJ Platform at runtime
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)

    testFixturesImplementation(libs.kotlin.stdlib)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
