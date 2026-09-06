plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Provided by the IntelliJ Platform at runtime
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)

    api(projects.components.host.api)
    api(projects.components.server.api)
    api(projects.components.harness.api)
    implementation(projects.components.core.api)

    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.tests.kotlin.test)
    testImplementation(testFixtures(projects.components.core.api))
}
