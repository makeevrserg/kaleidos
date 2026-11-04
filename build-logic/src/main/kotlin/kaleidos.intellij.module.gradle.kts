import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

plugins {
    id("org.jetbrains.intellij.platform.module")
}

extensions.configure<IntelliJPlatformExtension> {
    // No Swing forms and no @NotNull assertions to instrument; the task also fails on some macOS JDK layouts
    instrumentCode = false
}
