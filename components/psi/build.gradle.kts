plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind.set("technology")
    rootPackage.set("com.makeevrserg.kaleidos.psi")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
        // Kotlin PSI; the Kotlin plugin itself depends on the Java plugin
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")
    }

    implementation(projects.components.preview.api)
    implementation(projects.components.harness.api)
    implementation(projects.components.core.api)
    implementation(projects.components.core.intellij)
}
