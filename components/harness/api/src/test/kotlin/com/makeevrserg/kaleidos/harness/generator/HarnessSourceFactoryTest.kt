package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessFixtures.privatePreview
import com.makeevrserg.kaleidos.harness.HarnessFixtures.privatePreviewFile
import com.makeevrserg.kaleidos.harness.HarnessHostPlan
import com.makeevrserg.kaleidos.harness.HarnessLayout
import com.makeevrserg.kaleidos.harness.HarnessPlan
import com.makeevrserg.kaleidos.harness.PreviewModulePlan
import com.makeevrserg.kaleidos.host.DevServerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HarnessSourceFactoryTest {
    private val naming = HarnessNaming()

    private val factory = HarnessSourceFactory(
        registryFactory = PreviewRegistryFactory(naming),
        pageFactory = PreviewPageFactory(naming, PreviewPageSourceFactory(naming)),
        initScriptFactory = PreviewInitScriptFactory(HarnessLayout()),
        privatePreviewFileFactory = PrivatePreviewFileFactory(naming)
    )

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
        return sources.single { module -> module.moduleDirectory == directory }.files.map { file -> file.relativePath }
    }

    @Test
    fun GIVEN_dependency_with_private_previews_WHEN_create_THEN_its_sources_hold_the_registry_and_the_copy() {
        val module = moduleWithPrivatePreview(":components:ui", "/project/components/ui")

        val sources = factory.create(HarnessPlan(host = host, modules = listOf(module)))

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

        val sources = factory.create(HarnessPlan(host = host, modules = listOf(module)))

        assertTrue(
            "kotlin/kaleidos/sources/com/example/CardPreview.kt" in relativePathsIn(sources, host.directory),
            sources.toString()
        )
        assertEquals(1, sources.size)
    }
}
