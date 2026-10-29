plugins {
    id("kaleidos.kotlin.jvm")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind.set("api")
    rootPackage.set("com.makeevrserg.kaleidos")
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
}
