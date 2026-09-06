plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

dependencies {
    // Provided by the IntelliJ Platform at runtime
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)

    testFixturesImplementation(libs.kotlin.stdlib)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
