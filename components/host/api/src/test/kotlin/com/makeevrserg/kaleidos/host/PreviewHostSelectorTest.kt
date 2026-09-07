package com.makeevrserg.kaleidos.host

import com.makeevrserg.kaleidos.host.GradleModuleFixtures.jsBrowserModule
import com.makeevrserg.kaleidos.host.GradleModuleFixtures.kobwebApplication
import com.makeevrserg.kaleidos.host.GradleModuleFixtures.kobwebLibrary
import com.makeevrserg.kaleidos.host.GradleModuleFixtures.module
import com.makeevrserg.kaleidos.host.GradleModuleFixtures.structure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PreviewHostSelectorTest {
    private val selector = PreviewHostSelector()

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
        val resolution = selector.select("/project/components/ui", structure(emptyList()))

        assertEquals(PreviewHostSelector.GRADLE_NOT_IMPORTED, notFoundReason(resolution))
    }

    @Test
    fun GIVEN_file_module_with_browser_target_WHEN_select_THEN_it_previews_itself() {
        val fileModule = jsBrowserModule(":components:ui")

        val host = found(selector.select(fileModule.directory, structure(listOf(fileModule))))

        assertEquals(":components:ui", host.gradlePath)
        assertEquals(DevServerKind.WEBPACK, host.kind)
        assertEquals(GradleModuleFixtures.ROOT_PROJECT_PATH, host.rootProjectPath)
    }

    @Test
    fun GIVEN_browser_target_and_a_preview_application_WHEN_select_THEN_own_module_wins() {
        val fileModule = jsBrowserModule(":components:ui")
        val application = jsBrowserModule(":instances:web-preview")
        val structure = structure(listOf(fileModule, application), ":instances:web-preview" to ":components:ui")

        val host = found(selector.select(fileModule.directory, structure))

        assertEquals(":components:ui", host.gradlePath)
    }

    @Test
    fun GIVEN_kobweb_library_WHEN_select_THEN_the_application_that_depends_on_it_serves_it() {
        val fileModule = kobwebLibrary(":components:ui")
        val application = kobwebApplication(":instances:web-app")
        val structure = structure(listOf(fileModule, application), ":instances:web-app" to ":components:ui")

        val host = found(selector.select(fileModule.directory, structure))

        assertEquals(":instances:web-app", host.gradlePath)
        assertEquals(DevServerKind.KOBWEB, host.kind)
    }

    @Test
    fun GIVEN_kobweb_library_and_a_plain_browser_application_WHEN_select_THEN_kobweb_application_is_required() {
        val fileModule = kobwebLibrary(":components:ui")
        val plainApplication = jsBrowserModule(":instances:web-preview")
        val structure = structure(listOf(fileModule, plainApplication), ":instances:web-preview" to ":components:ui")

        val reason = notFoundReason(selector.select(fileModule.directory, structure))

        assertTrue(reason.contains("No Kobweb application"), reason)
    }

    @Test
    fun GIVEN_several_kobweb_applications_WHEN_select_THEN_the_one_named_preview_serves_it() {
        val fileModule = kobwebLibrary(":components:ui")
        val site = kobwebApplication(":instances:web-app")
        val previewApplication = kobwebApplication(":instances:web-preview")
        val structure = structure(
            listOf(fileModule, site, previewApplication),
            ":instances:web-app" to ":components:ui",
            ":instances:web-preview" to ":components:ui"
        )

        val host = found(selector.select(fileModule.directory, structure))

        assertEquals(":instances:web-preview", host.gradlePath)
    }

    @Test
    fun GIVEN_several_applications_without_a_preview_name_WHEN_select_THEN_the_choice_is_stable() {
        val fileModule = kobwebLibrary(":components:ui")
        val applications = listOf(kobwebApplication(":instances:web-tuner"), kobwebApplication(":instances:web-app"))
        val structure = structure(
            listOf(fileModule) + applications,
            ":instances:web-tuner" to ":components:ui",
            ":instances:web-app" to ":components:ui"
        )

        val host = found(selector.select(fileModule.directory, structure))

        assertEquals(":instances:web-app", host.gradlePath)
    }

    @Test
    fun GIVEN_module_without_a_browser_target_WHEN_select_THEN_a_module_that_depends_on_it_serves_it() {
        val fileModule = module(":components:shared", setOf("build"))
        val application = jsBrowserModule(":instances:web-app")
        val structure = structure(listOf(fileModule, application), ":instances:web-app" to ":components:shared")

        val host = found(selector.select(fileModule.directory, structure))

        assertEquals(":instances:web-app", host.gradlePath)
    }

    @Test
    fun GIVEN_nothing_can_render_the_file_WHEN_select_THEN_reason_names_the_module() {
        val fileModule = module(":components:shared", setOf("build"))
        val application = jsBrowserModule(":instances:web-app")
        val structure = structure(listOf(fileModule, application))

        val reason = notFoundReason(selector.select(fileModule.directory, structure))

        assertTrue(reason.startsWith(":components:shared has no Kotlin/JS browser target"), reason)
    }

    @Test
    fun GIVEN_transitive_dependency_WHEN_select_THEN_found() {
        val fileModule = kobwebLibrary(":components:ui")
        val feature = module(":components:feature")
        val application = kobwebApplication(":instances:web-app")
        val structure = structure(
            listOf(fileModule, feature, application),
            ":instances:web-app" to ":components:feature",
            ":components:feature" to ":components:ui"
        )

        val host = found(selector.select(fileModule.directory, structure))

        assertEquals(":instances:web-app", host.gradlePath)
    }
}
