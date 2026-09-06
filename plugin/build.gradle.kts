import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
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
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.gradle")
        pluginVerifier()
        // Merge :core into the plugin JAR; a plain project dependency lands in lib/modules and is not loaded
        pluginComposedModule(implementation(project(":core")))
    }
}

kotlin {
    compilerOptions {
        // Match the Kotlin runtime bundled with the target IntelliJ Platform
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        // Without JVM default methods Kotlin emits bridges to DefaultImpls of platform interfaces
        // (ToolWindowFactory and others), which the Plugin Verifier reports as internal API overrides
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

intellijPlatform {
    // No Swing forms and no @NotNull assertions to instrument; the task also fails on some macOS JDK layouts
    instrumentCode = false

    pluginConfiguration {
        version = providers.gradleProperty("klibs.project.version.string")
        ideaVersion {
            sinceBuild = providers.gradleProperty("intellij.sinceBuild")
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            // Verify against the same build the plugin is compiled with; add recommended() for a wider matrix
            current()
        }
    }
}

// Sandbox IDE with the Robot Server plugin for scripted UI checks. Pass -PuiTestProject=<path>
// to open a project on start: ./gradlew :plugin:runIdeForUiTests -PuiTestProject=/path/to/project
val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    task {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
                "-Didea.trust.all.projects=true"
            )
        }
        providers.gradleProperty("uiTestProject").orNull?.let { projectPath -> args(projectPath) }
    }

    plugins {
        robotServerPlugin()
    }
}
