package com.makeevrserg.compose.html.preview.host

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DevServerKindTest {

    @Test
    fun GIVEN_kobweb_and_webpack_tasks_WHEN_detect_THEN_kobweb_wins() {
        val kind = DevServerKind.detect(setOf("jsBrowserDevelopmentRun", "kobwebStart", "build"))

        assertEquals(DevServerKind.KOBWEB, kind)
    }

    @Test
    fun GIVEN_only_webpack_task_WHEN_detect_THEN_webpack() {
        val kind = DevServerKind.detect(setOf("jsBrowserDevelopmentRun", "build"))

        assertEquals(DevServerKind.WEBPACK, kind)
    }

    @Test
    fun GIVEN_no_dev_server_task_WHEN_detect_THEN_null() {
        assertNull(DevServerKind.detect(setOf("build", "jsBrowserProductionWebpack")))
    }
}
