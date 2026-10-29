@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.harness.generator.util

import com.makeevrserg.kaleidos.harness.generator.model.GeneratedFile
import com.makeevrserg.kaleidos.harness.generator.model.GeneratedModuleSources
import com.makeevrserg.kaleidos.harness.model.HarnessFixtures.privatePreview
import com.makeevrserg.kaleidos.harness.model.HarnessFixtures.privatePreviewFile
import com.makeevrserg.kaleidos.harness.model.HarnessHostPlan
import com.makeevrserg.kaleidos.harness.model.HarnessPlan
import com.makeevrserg.kaleidos.harness.model.PreviewModulePlan
import com.makeevrserg.kaleidos.host.model.DevServerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HarnessSourcesTest {

    private val host = HarnessHostPlan(
        gradlePath = ":instances:web-app",
        directory = "/project/instances/web-app",
        rootProjectPath = "/project",
        kind = DevServerKind.KOBWEB,
        devServerPort = null,
        excludedSourcePaths = emptyList()
    )

    private fun moduleWithPrivatePreview(gradlePath: String, directory: String): PreviewModulePlan {
        return PreviewModulePlan(
            gradlePath = gradlePath,
            directory = directory,
            previews = listOf(privatePreview("com.example.CardPreview")),
            privatePreviewFiles = listOf(privatePreviewFile("com.example.CardPreview"))
        )
    }

    private fun relativePathsIn(sources: List<GeneratedModuleSources>, directory: String): List<String> {
        return sources.single { module -> module.moduleDirectory == directory }.files.map(GeneratedFile::relativePath)
    }

    @Test
    fun GIVEN_dependency_with_private_previews_WHEN_create_THEN_its_sources_hold_the_registry_and_the_copy() {
        val module = moduleWithPrivatePreview(":components:ui", "/project/components/ui")

        val sources = HarnessPlan(host = host, modules = listOf(module)).generateHarnessSources()

        assertEquals(
            listOf(
                "kotlin/kaleidos/generated/m_components_ui/PreviewRegistry.kt",
                "kotlin/kaleidos/sources/com/example/CardPreview.kt"
            ),
            relativePathsIn(sources, module.directory)
        )
    }

    @Test
    fun GIVEN_host_with_private_previews_WHEN_create_THEN_the_copy_joins_the_page_of_the_host() {
        val module = moduleWithPrivatePreview(host.gradlePath, host.directory)

        val sources = HarnessPlan(host = host, modules = listOf(module)).generateHarnessSources()

        assertTrue(
            "kotlin/kaleidos/sources/com/example/CardPreview.kt" in relativePathsIn(sources, host.directory),
            sources.toString()
        )
        assertEquals(1, sources.size)
    }
}
