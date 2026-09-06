plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    // Provided by the IntelliJ Platform at runtime
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.coroutines.core)
}
