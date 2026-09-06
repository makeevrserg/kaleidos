package com.makeevrserg.compose.html.preview.harness.generator

import com.makeevrserg.compose.html.preview.harness.HarnessHostPlan
import com.makeevrserg.compose.html.preview.harness.HarnessPlan
import com.makeevrserg.compose.html.preview.harness.HarnessPreview
import com.makeevrserg.compose.html.preview.harness.PreviewModulePlan
import com.makeevrserg.compose.html.preview.harness.PreviewPagePath
import com.makeevrserg.compose.html.preview.host.DevServerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreviewPageFactoryTest {
    private val factory = PreviewPageFactory(HarnessNaming(), PreviewPageSourceFactory(HarnessNaming()))

    private val uiModule = PreviewModulePlan(
        gradlePath = ":components:ui",
        directory = "/project/components/ui",
        previews = listOf(HarnessPreview("com.example.CardPreview"))
    )

    private fun plan(kind: DevServerKind, modules: List<PreviewModulePlan> = listOf(uiModule)): HarnessPlan {
        return HarnessPlan(
            host = HarnessHostPlan(
                gradlePath = ":instances:web-app",
                directory = "/project/instances/web-app",
                rootProjectPath = "/project",
                kind = kind,
                devServerPort = if (kind == DevServerKind.WEBPACK) 8301 else null,
                excludedSourcePaths = emptyList()
            ),
            modules = modules
        )
    }

    private fun fileNames(kind: DevServerKind): List<String> {
        return factory.create(plan(kind)).map { file -> file.relativePath.substringAfterLast('/') }
    }

    private fun pageContent(kind: DevServerKind, modules: List<PreviewModulePlan> = listOf(uiModule)): String {
        return factory.create(plan(kind, modules))
            .first { file -> file.relativePath.endsWith("PreviewPage.kt") }
            .content
    }

    @Test
    fun GIVEN_webpack_host_WHEN_create_THEN_the_page_comes_with_an_entry_point_and_a_document() {
        assertEquals(
            listOf("PreviewPage.kt", "PreviewMain.kt", PreviewPagePath.WEBPACK_FILE_NAME),
            fileNames(DevServerKind.WEBPACK)
        )
    }

    @Test
    fun GIVEN_kobweb_host_WHEN_create_THEN_the_page_is_reached_through_a_route() {
        assertEquals(listOf("PreviewPage.kt", "ComposeHtmlPreviewRoute.kt"), fileNames(DevServerKind.KOBWEB))

        val route = factory.create(plan(DevServerKind.KOBWEB))
            .first { file -> file.relativePath.endsWith("ComposeHtmlPreviewRoute.kt") }

        assertTrue(route.content.contains("@Page(\"${PreviewPagePath.KOBWEB_ROUTE}\")"), route.content)
    }

    @Test
    fun GIVEN_webpack_host_WHEN_create_THEN_the_document_loads_the_bundle_of_the_preview() {
        val document = factory.create(plan(DevServerKind.WEBPACK))
            .first { file -> file.relativePath.endsWith(PreviewPagePath.WEBPACK_FILE_NAME) }

        assertEquals("resources/${PreviewPagePath.WEBPACK_FILE_NAME}", document.relativePath)
        assertTrue(
            document.content.contains("<script src=\"${PreviewPagePath.WEBPACK_BUNDLE_NAME}\">"),
            document.content
        )
    }

    @Test
    fun GIVEN_modules_with_previews_WHEN_create_THEN_the_page_asks_every_registry() {
        val content = pageContent(DevServerKind.KOBWEB)

        assertTrue(
            content.contains("    if (fqn in composehtmlpreview.generated.m_components_ui.PreviewRegistry.fqns) {"),
            content
        )
        assertTrue(
            content.contains("    composehtmlpreview.generated.m_components_ui.PreviewRegistry.fqns"),
            content
        )
    }

    @Test
    fun GIVEN_no_previews_at_all_WHEN_create_THEN_the_page_still_compiles() {
        val content = pageContent(DevServerKind.KOBWEB, modules = emptyList())

        assertTrue(content.contains("private val allPreviewFqns: List<String> = emptyList()"), content)
    }
}
