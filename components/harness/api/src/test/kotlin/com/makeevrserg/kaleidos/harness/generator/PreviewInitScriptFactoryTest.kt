package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessHostPlan
import com.makeevrserg.kaleidos.harness.HarnessLayout
import com.makeevrserg.kaleidos.harness.HarnessPlan
import com.makeevrserg.kaleidos.harness.HarnessPreview
import com.makeevrserg.kaleidos.harness.PreviewModulePlan
import com.makeevrserg.kaleidos.host.DevServerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreviewInitScriptFactoryTest {
    private val factory = PreviewInitScriptFactory(HarnessLayout())

    private val uiModule = PreviewModulePlan(
        gradlePath = ":components:ui",
        directory = "/project/components/ui",
        previews = listOf(HarnessPreview("com.example.CardPreview"))
    )

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

    @Test
    fun GIVEN_plan_WHEN_create_THEN_it_is_the_init_script_of_the_host() {
        val file = factory.create(plan(DevServerKind.KOBWEB))

        assertEquals(HarnessLayout.INIT_SCRIPT_NAME, file.relativePath)
    }

    @Test
    fun GIVEN_module_with_previews_WHEN_create_THEN_its_generated_sources_are_added_to_the_build() {
        val content = factory.create(plan(DevServerKind.KOBWEB)).content

        assertTrue(content.contains("':components:ui': ["), content)
        assertTrue(
            content.contains("kotlinSrcDir: '/project/components/ui/build/kaleidos/kotlin'"),
            content
        )
    }

    @Test
    fun GIVEN_kobweb_host_WHEN_create_THEN_the_module_is_not_turned_into_an_application() {
        val content = factory.create(plan(DevServerKind.KOBWEB)).content

        assertTrue(content.contains("devServerPort: null"), content)
    }

    @Test
    fun GIVEN_webpack_host_WHEN_create_THEN_it_gets_the_port_the_plugin_chose_and_a_document() {
        val content = factory.create(plan(DevServerKind.WEBPACK, listOf("com/example/Main.kt"))).content

        assertTrue(content.contains("devServerPort: 8301"), content)
        assertTrue(
            content.contains("resourcesSrcDir: '/project/instances/web-app/build/kaleidos/resources'"),
            content
        )
        assertTrue(content.contains("excludedSourcePaths: ['com/example/Main.kt']"), content)
    }

    @Test
    fun GIVEN_plan_WHEN_create_THEN_the_map_of_injections_is_valid_groovy() {
        val content = factory.create(plan(DevServerKind.KOBWEB)).content
        val injections = content.substringAfter("def previewInjections = [").substringBefore("\n]")

        assertFalse(injections.trimEnd().endsWith(","), injections)
    }
}
