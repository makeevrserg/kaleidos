import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform.module)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("intellij.version"))
    }
}

kotlin {
    compilerOptions {
        // Match the Kotlin runtime bundled with the target IntelliJ Platform
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        // Same JVM default mode as :plugin, the modules are merged into one JAR
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

intellijPlatform {
    // No Swing forms and no @NotNull assertions to instrument; the task also fails on some macOS JDK layouts
    instrumentCode = false
}
