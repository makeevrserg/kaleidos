plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.intellij.platform.gradle.plugin)
    implementation(libs.intellij.platform.module.gradle.plugin)
    implementation(libs.klibs.gradle.java.version.plugin)
    implementation(libs.kotlin.jvm.gradle.plugin)
}
