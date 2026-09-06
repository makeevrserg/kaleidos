import org.gradle.process.CommandLineArgumentProvider

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.gradle")
        bundledModules("intellij.platform.ui.jcef", "intellij.libraries.jcef")
        pluginVerifier()
        // Every component is merged into the plugin JAR; a plain project dependency lands in lib/modules
        // and is not loaded. The list is explicit so no module is pulled in by accident.
        pluginComposedModule(implementation(projects.components.core.api))
        pluginComposedModule(implementation(projects.components.core.intellij))
        pluginComposedModule(implementation(projects.components.host.api))
        pluginComposedModule(implementation(projects.components.host.intellij))
        pluginComposedModule(implementation(projects.components.server.api))
        pluginComposedModule(implementation(projects.components.server.intellij))
        pluginComposedModule(implementation(projects.components.preview.api))
        pluginComposedModule(implementation(projects.components.preview.intellij))
        pluginComposedModule(implementation(projects.components.psi))
        pluginComposedModule(implementation(projects.components.ui))
    }
}

intellijPlatform {
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

// `buildPlugin` names the ZIP after the Gradle project name and version, which are `plugin` and unset here,
// so the distribution would be `plugin.zip`. Release assets must carry the plugin name and version.
tasks.named<Zip>("buildPlugin") {
    archiveBaseName = providers.gradleProperty("klibs.project.name")
    archiveVersion = providers.gradleProperty("klibs.project.version.string")
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
