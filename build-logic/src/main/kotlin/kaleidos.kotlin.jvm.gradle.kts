import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.jetbrains.kotlin.jvm")
}

apply(plugin = "ru.astrainteractive.gradleplugin.java.version")

extensions.configure<KotlinJvmProjectExtension> {
    compilerOptions {
        // Match the Kotlin runtime bundled with the target IntelliJ Platform
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        // Without JVM default methods Kotlin emits bridges to DefaultImpls of platform interfaces
        // (ToolWindowFactory and others), which the Plugin Verifier reports as internal API overrides.
        // Every module is merged into one JAR, so all of them use the same mode.
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
