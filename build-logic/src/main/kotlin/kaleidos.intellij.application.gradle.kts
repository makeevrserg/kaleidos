import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

plugins {
    id("org.jetbrains.intellij.platform")
}

extensions.configure<IntelliJPlatformExtension> {
    instrumentCode = false
}
