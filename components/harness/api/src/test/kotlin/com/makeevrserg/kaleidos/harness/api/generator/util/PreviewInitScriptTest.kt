@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.harness.api.generator.util

import com.makeevrserg.kaleidos.harness.api.model.HarnessFixtures.preview
import com.makeevrserg.kaleidos.harness.api.model.HarnessFixtures.privatePreview
import com.makeevrserg.kaleidos.harness.api.model.HarnessFixtures.privatePreviewFile
import com.makeevrserg.kaleidos.harness.api.model.HarnessHostPlan
import com.makeevrserg.kaleidos.harness.api.model.HarnessPlan
import com.makeevrserg.kaleidos.harness.api.model.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.api.util.INIT_SCRIPT_NAME
import com.makeevrserg.kaleidos.harness.api.util.harnessKotlinRoot
import com.makeevrserg.kaleidos.host.api.model.DevServerKind
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreviewInitScriptTest {

    private val uiModule = PreviewModulePlan(
        gradlePath = ":components:ui",
        directory = "/project/components/ui",
        previews = listOf(preview("com.example.CardPreview")),
        privatePreviewFiles = emptyList()
    )

    private val uiModuleWithPrivatePreviews = uiModule.copy(
        previews = listOf(privatePreview("com.example.CardPreview")),
        privatePreviewFiles = listOf(privatePreviewFile("com.example.CardPreview"))
    )

    private fun injectionOf(content: String, gradlePath: String): String {
        return content.substringAfter("'$gradlePath': [").substringBefore("    ]")
    }

    private fun plan(kind: DevServerKind, excludedSourcePaths: List<String> = emptyList()): HarnessPlan {
        return HarnessPlan(
            host = HarnessHostPlan(
                gradlePath = ":instances:web-app",
                directory = "/project/instances/web-app",
                rootProjectPath = "/project",
                kind = kind,
                devServerPort = if (kind == DevServerKind.WEBPACK) 8301 else null,
                excludedSourcePaths = excludedSourcePaths
            ),
            modules = listOf(uiModule)
        )
    }

    private fun writeCompilationFixture(directory: File, pluginId: String, hostPath: String) {
        val configuration = if (pluginId == "org.jetbrains.kotlin.multiplatform") {
            "jsMainImplementation"
        } else {
            "implementation"
        }
        directory.resolve("settings.gradle").writeText(
            "rootProject.name = 'preview-fixture'\ninclude ':host', ':feature', ':core', ':unrelated'\n"
        )
        directory.resolve("build.gradle").writeText(
            """
            plugins { id '$pluginId' version '2.3.20' }
            allprojects {
                apply plugin: '$pluginId'
                repositories { mavenCentral() }
                kotlin { js { browser() } }
            }
            project(':feature') {
                dependencies { add('$configuration', project(':core')) }
            }
            project('$hostPath') {
                dependencies { add('$configuration', project(':feature')) }
            }
            """.trimIndent()
        )
        listOf(
            "host",
            "feature",
            "core",
            "unrelated"
        ).forEach { name -> directory.resolve(name).mkdirs() }
    }

    private fun compilationPlan(directory: File, hostPath: String): HarnessPlan {
        val base = plan(DevServerKind.KOBWEB)
        val hostDirectory = if (hostPath == ":") directory else directory.resolve("host")
        val modules = listOf(
            ":core",
            ":feature",
            hostPath
        ).map { path ->
            uiModule.copy(
                gradlePath = path,
                directory = if (path == ":") directory.path else directory.resolve(path.removePrefix(":")).path
            )
        }
        return base.copy(
            host = base.host.copy(
                gradlePath = hostPath,
                directory = hostDirectory.path,
                rootProjectPath = directory.path
            ),
            modules = modules
        )
    }

    private fun writeRegistryFixture(plan: HarnessPlan) {
        plan.modules.forEach { module ->
            val root = harnessKotlinRoot(module.directory).toFile()
            root.mkdirs()
            root.resolve("PreviewRegistry.kt").writeText(
                """
                package ${registryPackage(module.gradlePath)}
                object PreviewRegistry { val fqns: List<String> = listOf("${module.gradlePath}") }
                """.trimIndent()
            )
        }
        val references = plan.modules.joinToString(" + ") { module -> "${registryFqn(module.gradlePath)}.fqns" }
        harnessKotlinRoot(plan.host.directory).resolve("PreviewPage.kt").toFile().writeText(
            "package kaleidos.generated\nval allPreviewFqns: List<String> = $references\n"
        )
    }

    private fun verifyRegistryCompilation(pluginId: String, hostPath: String) {
        val directory = Files.createTempDirectory("kaleidos preview's ").toFile().canonicalFile
        try {
            writeCompilationFixture(
                directory,
                pluginId,
                hostPath
            )
            val plan = compilationPlan(directory, hostPath)
            writeRegistryFixture(plan)
            val script = directory.resolve(INIT_SCRIPT_NAME)
            script.writeText(plan.generatePreviewInitScript().content)
            val task = if (hostPath == ":") ":compileKotlinJs" else "$hostPath:compileKotlinJs"
            val runner = GradleRunner.create().withProjectDir(directory).withArguments(
                task,
                "--init-script",
                script.path,
                "--configuration-cache",
                "--stacktrace"
            )

            val first = runner.build()
            val second = runner.build()

            assertEquals(TaskOutcome.SUCCESS, first.task(task)?.outcome)
            assertEquals(TaskOutcome.UP_TO_DATE, second.task(task)?.outcome)
            assertTrue(second.output.contains("Reusing configuration cache."), second.output)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun GIVEN_transitive_implementation_dependency_WHEN_compile_preview_THEN_all_registries_are_visible() {
        verifyRegistryCompilation("org.jetbrains.kotlin.multiplatform", ":host")
    }

    @Test
    fun GIVEN_root_js_host_with_own_previews_WHEN_compile_preview_THEN_no_self_dependency_is_added() {
        verifyRegistryCompilation("org.jetbrains.kotlin.js", ":")
    }

    @Test
    fun GIVEN_plan_WHEN_create_THEN_it_is_the_init_script_of_the_host() {
        val file = plan(DevServerKind.KOBWEB).generatePreviewInitScript()

        assertEquals(INIT_SCRIPT_NAME, file.relativePath)
    }

    @Test
    fun GIVEN_module_with_previews_WHEN_create_THEN_its_generated_sources_are_added_to_the_build() {
        val content = plan(DevServerKind.KOBWEB).generatePreviewInitScript().content

        assertTrue(content.contains("':components:ui': ["), content)
        assertTrue(
            content.contains("kotlinSrcDir: '/project/components/ui/build/kaleidos/kotlin'"),
            content
        )
    }

    @Test
    fun GIVEN_kobweb_host_WHEN_create_THEN_the_module_is_not_turned_into_an_application() {
        val content = plan(DevServerKind.KOBWEB).generatePreviewInitScript().content

        assertTrue(content.contains("devServerPort: null"), content)
    }

    @Test
    fun GIVEN_webpack_host_WHEN_create_THEN_it_gets_the_port_the_plugin_chose_and_a_document() {
        val content = plan(DevServerKind.WEBPACK, listOf("com/example/Main.kt")).generatePreviewInitScript().content

        assertTrue(content.contains("devServerPort: 8301"), content)
        assertTrue(
            content.contains("resourcesSrcDir: '/project/instances/web-app/build/kaleidos/resources'"),
            content
        )
        assertTrue(content.contains("excludedSourcePaths: ['com/example/Main.kt']"), content)
    }

    @Test
    fun GIVEN_dependency_with_private_previews_WHEN_create_THEN_their_originals_are_left_out_of_its_build() {
        val plan = plan(DevServerKind.KOBWEB).copy(modules = listOf(uiModuleWithPrivatePreviews))

        val content = plan.generatePreviewInitScript().content

        assertTrue(
            injectionOf(content, ":components:ui").contains("excludedSourcePaths: ['com/example/CardPreview.kt']"),
            content
        )
        assertTrue(injectionOf(content, ":instances:web-app").contains("excludedSourcePaths: []"), content)
    }

    @Test
    fun GIVEN_host_with_an_entry_point_and_private_previews_WHEN_create_THEN_both_are_left_out_of_its_build() {
        val base = plan(DevServerKind.WEBPACK, listOf("com/example/Main.kt"))
        val hostModule = uiModuleWithPrivatePreviews.copy(
            gradlePath = base.host.gradlePath,
            directory = base.host.directory
        )

        val content = base.copy(modules = listOf(hostModule)).generatePreviewInitScript().content

        assertTrue(
            injectionOf(content, ":instances:web-app")
                .contains("excludedSourcePaths: ['com/example/Main.kt', 'com/example/CardPreview.kt']"),
            content
        )
    }

    @Test
    fun GIVEN_plan_WHEN_create_THEN_exclusions_reach_every_source_set_and_not_only_the_injected_one() {
        val content = plan(DevServerKind.KOBWEB).generatePreviewInitScript().content

        val exclusion = content.indexOf("injection.excludedSourcePaths.each")
        val injectedSourceSetOnly = content.indexOf("if (sourceSet.name != sourceSetName) return")
        assertTrue(exclusion in 0 until injectedSourceSetOnly, content)
    }

    @Test
    fun GIVEN_plan_WHEN_create_THEN_the_map_of_injections_is_valid_groovy() {
        val content = plan(DevServerKind.KOBWEB).generatePreviewInitScript().content
        val injections = content.substringAfter("def previewInjections = [").substringBefore("\n]")

        assertFalse(injections.trimEnd().endsWith(","), injections)
    }
}
