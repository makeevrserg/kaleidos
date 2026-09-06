package com.makeevrserg.compose.html.preview.host

import com.makeevrserg.compose.html.preview.host.GradleModuleFixtures.graph
import com.makeevrserg.compose.html.preview.host.GradleModuleFixtures.kobwebModule
import com.makeevrserg.compose.html.preview.host.GradleModuleFixtures.module
import com.makeevrserg.compose.html.preview.host.GradleModuleFixtures.webpackModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PreviewHostSelectorTest {
    private val selector = PreviewHostSelector()

    private val fileModule = module(":components:ui")

    private fun found(resolution: PreviewHostResolution): PreviewHost {
        assertIs<PreviewHostResolution.Found>(resolution)
        return resolution.host
    }

    private fun notFoundReason(resolution: PreviewHostResolution): String {
        assertIs<PreviewHostResolution.NotFound>(resolution)
        return resolution.reason
    }

    @Test
    fun GIVEN_no_modules_WHEN_select_THEN_gradle_not_imported() {
        val resolution = selector.select(fileModule.directory, emptyList(), graph())

        assertEquals(PreviewHostSelector.GRADLE_NOT_IMPORTED, notFoundReason(resolution))
    }

    @Test
    fun GIVEN_no_module_with_dev_server_WHEN_select_THEN_asks_for_preview_module() {
        val modules = listOf(fileModule, module(":app", setOf("build")))

        val reason = notFoundReason(selector.select(fileModule.directory, modules, graph(":app" to ":components:ui")))

        assertTrue(reason.startsWith("No Kotlin/JS browser or Kobweb module found"), reason)
    }

    @Test
    fun GIVEN_file_module_has_dev_server_WHEN_select_THEN_own_module_is_host() {
        val ownModule = webpackModule(":components:ui")

        val host = found(selector.select(ownModule.directory, listOf(ownModule), graph()))

        assertEquals(":components:ui", host.gradlePath)
        assertEquals(DevServerKind.WEBPACK, host.kind)
        assertEquals(GradleModuleFixtures.ROOT_PROJECT_PATH, host.rootProjectPath)
    }

    @Test
    fun GIVEN_server_module_depends_transitively_on_file_module_WHEN_select_THEN_found() {
        val modules = listOf(fileModule, module(":components:feature"), webpackModule(":instances:web-preview"))
        val dependencies = graph(
            ":instances:web-preview" to ":components:feature",
            ":components:feature" to ":components:ui"
        )

        val host = found(selector.select(fileModule.directory, modules, dependencies))

        assertEquals(":instances:web-preview", host.gradlePath)
    }

    @Test
    fun GIVEN_server_module_does_not_depend_on_file_module_WHEN_select_THEN_lists_servers() {
        val modules = listOf(fileModule, webpackModule(":instances:web-app"))

        val reason = notFoundReason(selector.select(fileModule.directory, modules, graph()))

        assertTrue(reason.startsWith("No module with a dev server depends on the module of this file"), reason)
        assertTrue(reason.contains(":instances:web-app"), reason)
    }

    @Test
    fun GIVEN_real_app_and_preview_module_both_qualify_WHEN_select_THEN_preview_module_wins() {
        val modules = listOf(fileModule, webpackModule(":instances:web-app"), kobwebModule(":instances:web-preview"))
        val dependencies = graph(
            ":instances:web-app" to ":components:ui",
            ":instances:web-preview" to ":components:ui"
        )

        val host = found(selector.select(fileModule.directory, modules, dependencies))

        assertEquals(":instances:web-preview", host.gradlePath)
        assertEquals(DevServerKind.KOBWEB, host.kind)
    }

    @Test
    fun GIVEN_preview_marker_matches_case_insensitively_WHEN_select_THEN_preview_module_wins() {
        val modules = listOf(fileModule, webpackModule(":instances:web-app"), webpackModule(":instances:WebPreview"))
        val dependencies = graph(
            ":instances:web-app" to ":components:ui",
            ":instances:WebPreview" to ":components:ui"
        )

        val host = found(selector.select(fileModule.directory, modules, dependencies))

        assertEquals(":instances:WebPreview", host.gradlePath)
    }

    @Test
    fun GIVEN_several_preview_modules_including_the_files_own_WHEN_select_THEN_own_module_wins() {
        val ownModule = webpackModule(":preview:samples")
        val modules = listOf(ownModule, webpackModule(":preview:gallery"))
        val dependencies = graph(":preview:gallery" to ":preview:samples")

        val host = found(selector.select(ownModule.directory, modules, dependencies))

        assertEquals(":preview:samples", host.gradlePath)
    }

    @Test
    fun GIVEN_several_qualifying_modules_without_preview_marker_WHEN_select_THEN_ambiguous() {
        val modules = listOf(fileModule, webpackModule(":instances:web-app"), webpackModule(":instances:web-admin"))
        val dependencies = graph(
            ":instances:web-app" to ":components:ui",
            ":instances:web-admin" to ":components:ui"
        )

        val reason = notFoundReason(selector.select(fileModule.directory, modules, dependencies))

        assertTrue(reason.startsWith("Several modules could serve the preview"), reason)
        assertTrue(reason.contains(":instances:web-app") && reason.contains(":instances:web-admin"), reason)
    }

    @Test
    fun GIVEN_root_project_serves_previews_WHEN_select_THEN_host_has_empty_gradle_path_and_root_display_name() {
        val rootModule = webpackModule("")
        val modules = listOf(fileModule, rootModule)
        val dependencies = graph("" to ":components:ui")

        val host = found(selector.select(fileModule.directory, modules, dependencies))

        assertEquals("", host.gradlePath)
        assertEquals("root project", host.displayName)
    }
}
