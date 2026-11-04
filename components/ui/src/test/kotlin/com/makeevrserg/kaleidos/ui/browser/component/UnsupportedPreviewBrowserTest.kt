@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.ui.browser.component

import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsupportedPreviewBrowserTest {
    @Test
    fun GIVEN_unavailable_browser_WHEN_pages_change_THEN_each_page_finishes_and_clear_emits_nothing(): TestResult {
        return runTest {
            val browser = UnsupportedPreviewBrowser()
            val urls = flowOf(
                null,
                "http://localhost/first",
                null,
                "http://localhost/second"
            )

            val results = browser.observePages(urls).toList()

            assertEquals(listOf(PageLoad.Succeeded, PageLoad.Succeeded), results)
        }
    }
}
