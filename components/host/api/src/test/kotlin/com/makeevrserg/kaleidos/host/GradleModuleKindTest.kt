package com.makeevrserg.kaleidos.host

import kotlin.test.Test
import kotlin.test.assertEquals

class GradleModuleKindTest {

    @Test
    fun GIVEN_kobweb_application_tasks_WHEN_detect_THEN_application_wins_over_browser_target() {
        val kind = GradleModuleKind.detect(setOf("jsBrowserTest", "kobwebStart", "build"))

        assertEquals(GradleModuleKind.KOBWEB_APPLICATION, kind)
    }

    @Test
    fun GIVEN_kobweb_library_tasks_WHEN_detect_THEN_library_cannot_serve_a_page() {
        val kind = GradleModuleKind.detect(setOf("jsBrowserTest", "kobwebGenerateLibraryMetadata"))

        assertEquals(GradleModuleKind.KOBWEB_LIBRARY, kind)
        assertEquals(null, kind.devServerKind)
    }

    @Test
    fun GIVEN_browser_target_only_WHEN_detect_THEN_module_serves_with_webpack() {
        val kind = GradleModuleKind.detect(setOf("jsBrowserTest", "build"))

        assertEquals(GradleModuleKind.JS_BROWSER, kind)
        assertEquals(DevServerKind.WEBPACK, kind.devServerKind)
    }

    @Test
    fun GIVEN_jvm_module_tasks_WHEN_detect_THEN_other() {
        assertEquals(GradleModuleKind.OTHER, GradleModuleKind.detect(setOf("build", "jar")))
    }
}
