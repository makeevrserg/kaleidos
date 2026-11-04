@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.ui.browser.util

import com.makeevrserg.kaleidos.harness.api.util.TITLE
import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import com.makeevrserg.kaleidos.ui.browser.fake.FakeBrowserPageRegistration
import com.makeevrserg.kaleidos.ui.browser.model.BrowserPageEvent
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class FlowExtTest {
    private fun Flow<String?>.observe(registration: FakeBrowserPageRegistration): Flow<PageLoad> {
        return observeBrowserPages(
            registerLoadHandler = registration::registerLoadHandler,
            registerTitleHandler = registration::registerTitleHandler,
            navigate = registration::navigate,
            identificationTimeout = 1.seconds
        )
    }

    @Test
    fun GIVEN_immediate_native_load_WHEN_navigation_begins_THEN_handlers_are_already_registered(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            native.navigationEvents = listOf(
                BrowserPageEvent.Started(true),
                BrowserPageEvent.TitleChanged(TITLE),
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            val results = mutableListOf<PageLoad>()
            val observation = flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()

            assertEquals(
                listOf(
                    "register-load",
                    "register-title",
                    "navigate"
                ),
                native.calls
            )
            assertEquals(listOf<PageLoad>(PageLoad.Succeeded), results)
            observation.cancelAndJoin()
            assertEquals(listOf("remove-title", "remove-load"), native.calls.takeLast(2))
            assertTrue(checkNotNull(native.events).trySend(BrowserPageEvent.TitleChanged(TITLE)).isFailure)
        }
    }

    @Test
    fun GIVEN_pending_title_WHEN_matching_title_arrives_THEN_success_without_later_timeout(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            advanceTimeBy(500)
            native.emit(BrowserPageEvent.TitleChanged(TITLE))
            runCurrent()
            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(listOf<PageLoad>(PageLoad.Succeeded), results)
        }
    }

    @Test
    fun GIVEN_foreign_title_WHEN_identification_expires_THEN_foreign_page(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(BrowserPageEvent.TitleChanged("Other application"))
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(listOf<PageLoad>(PageLoad.Foreign), results)
        }
    }

    @Test
    fun GIVEN_pending_identification_WHEN_url_is_cleared_THEN_old_result_is_cancelled(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val urls = MutableStateFlow<String?>(PAGE_URL)
            val results = mutableListOf<PageLoad>()
            urls.observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            urls.value = null
            runCurrent()
            native.emit(BrowserPageEvent.TitleChanged(TITLE))
            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(listOf(PAGE_URL, null), native.navigatedUrls)
            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun GIVEN_queued_identification_WHEN_next_native_load_starts_THEN_stale_success_is_rejected(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(BrowserPageEvent.TitleChanged(TITLE))
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            native.emit(BrowserPageEvent.Started(true))
            runCurrent()
            advanceTimeBy(2_000)
            runCurrent()

            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun GIVEN_successful_page_WHEN_next_url_has_no_title_THEN_previous_title_is_not_reused(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val urls = MutableStateFlow<String?>(PAGE_URL)
            val results = mutableListOf<PageLoad>()
            urls.observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(BrowserPageEvent.TitleChanged(TITLE))
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            urls.value = "$PAGE_URL/another"
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    urls.value,
                    200
                )
            )
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(listOf<PageLoad>(PageLoad.Succeeded, PageLoad.Foreign), results)
        }
    }

    @Test
    fun GIVEN_pending_identification_WHEN_http_failure_arrives_THEN_failure_replaces_timeout(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    404
                )
            )
            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(listOf<PageLoad>(PageLoad.Failed("The dev server answered HTTP 404")), results)
        }
    }

    @Test
    fun GIVEN_transport_errors_WHEN_reported_THEN_text_or_error_code_is_used(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            native.emit(
                BrowserPageEvent.Failed(
                    true,
                    false,
                    "Connection refused",
                    "REFUSED"
                )
            )
            native.emit(
                BrowserPageEvent.Failed(
                    true,
                    false,
                    " ",
                    "EMPTY"
                )
            )
            native.emit(
                BrowserPageEvent.Failed(
                    true,
                    false,
                    null,
                    "MISSING"
                )
            )
            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(
                listOf<PageLoad>(
                    PageLoad.Failed("Connection refused"),
                    PageLoad.Failed("EMPTY"),
                    PageLoad.Failed("MISSING")
                ),
                results
            )
        }
    }

    @Test
    fun GIVEN_pending_page_WHEN_unrelated_native_events_arrive_THEN_identification_keeps_its_deadline(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            advanceTimeBy(500)
            listOf(
                BrowserPageEvent.Started(false),
                BrowserPageEvent.Failed(
                    false,
                    false,
                    "subframe",
                    "FAILED"
                ),
                BrowserPageEvent.Failed(
                    true,
                    true,
                    "aborted",
                    "ABORTED"
                ),
                BrowserPageEvent.Finished(
                    false,
                    PAGE_URL,
                    404
                ),
                BrowserPageEvent.Finished(
                    true,
                    "about:blank",
                    200
                ),
                BrowserPageEvent.Finished(
                    true,
                    null,
                    200
                ),
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    0
                ),
                BrowserPageEvent.TitleChanged(null)
            ).forEach(native::emit)
            advanceTimeBy(500)
            runCurrent()

            assertEquals(listOf<PageLoad>(PageLoad.Foreign), results)
        }
    }

    @Test
    fun GIVEN_pending_identification_WHEN_collection_is_cancelled_THEN_timeout_and_handlers_are_removed(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            val observation = flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            native.emit(
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                )
            )
            runCurrent()
            observation.cancelAndJoin()
            advanceTimeBy(2_000)
            runCurrent()

            assertTrue(results.isEmpty())
            assertEquals(1, native.calls.count { call -> call == "remove-load" })
            assertEquals(1, native.calls.count { call -> call == "remove-title" })
        }
    }

    @Test
    fun GIVEN_title_registration_fails_WHEN_observed_THEN_load_handler_is_removed(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            native.titleRegistrationFailure = IllegalStateException("title registration")

            assertFailsWith<IllegalStateException> { flowOf(PAGE_URL).observe(native).toList() }

            assertEquals(listOf("register-load", "remove-load"), native.calls)
            assertTrue(native.navigatedUrls.isEmpty())
            assertTrue(checkNotNull(native.events).trySend(BrowserPageEvent.TitleChanged(TITLE)).isFailure)
        }
    }

    @Test
    fun GIVEN_first_registration_fails_WHEN_observed_THEN_no_other_registration_or_navigation_occurs(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            native.loadRegistrationFailure = IllegalStateException("load registration")

            assertFailsWith<IllegalStateException> { flowOf(PAGE_URL).observe(native).toList() }

            assertTrue(native.calls.isEmpty())
            assertTrue(native.navigatedUrls.isEmpty())
        }
    }

    @Test
    fun GIVEN_navigation_fails_WHEN_observed_THEN_both_handlers_are_removed(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            native.navigationFailure = IllegalStateException("navigation")

            assertFailsWith<IllegalStateException> { flowOf(PAGE_URL).observe(native).toList() }

            assertEquals(
                listOf(
                    "register-load",
                    "register-title",
                    "remove-title",
                    "remove-load"
                ),
                native.calls
            )
        }
    }

    @Test
    fun GIVEN_cleared_page_WHEN_late_native_callbacks_arrive_THEN_no_load_is_reported(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val urls = MutableStateFlow<String?>(PAGE_URL)
            val results = mutableListOf<PageLoad>()
            urls.observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            urls.value = null
            runCurrent()
            listOf(
                BrowserPageEvent.Started(true),
                BrowserPageEvent.TitleChanged(TITLE),
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    200
                ),
                BrowserPageEvent.Finished(
                    true,
                    PAGE_URL,
                    404
                ),
                BrowserPageEvent.Failed(
                    true,
                    false,
                    "late error",
                    "FAILED"
                )
            ).forEach(native::emit)
            advanceTimeBy(2_000)
            runCurrent()

            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun GIVEN_unchanged_url_WHEN_browser_reloads_THEN_each_native_load_is_reported(): TestResult {
        return runTest {
            val native = FakeBrowserPageRegistration()
            val results = mutableListOf<PageLoad>()
            flowOf(PAGE_URL).observe(native).onEach(results::add).launchIn(backgroundScope)
            runCurrent()
            repeat(2) {
                native.emit(BrowserPageEvent.Started(true))
                native.emit(BrowserPageEvent.TitleChanged(TITLE))
                native.emit(
                    BrowserPageEvent.Finished(
                        true,
                        PAGE_URL,
                        200
                    )
                )
                runCurrent()
            }

            assertEquals(listOf<String?>(PAGE_URL), native.navigatedUrls)
            assertEquals(listOf<PageLoad>(PageLoad.Succeeded, PageLoad.Succeeded), results)
        }
    }

    private companion object {
        const val PAGE_URL = "http://localhost:8080/preview"
    }
}
