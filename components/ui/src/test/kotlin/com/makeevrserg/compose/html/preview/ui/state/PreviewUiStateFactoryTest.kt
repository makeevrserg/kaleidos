package com.makeevrserg.compose.html.preview.ui.state

import com.makeevrserg.compose.html.preview.feature.PageState
import com.makeevrserg.compose.html.preview.feature.PreviewScan
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.ui.PreviewStateTexts
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreviewUiStateFactoryTest {

    private val texts = PreviewStateTexts()

    private val factory = PreviewUiStateFactory(texts)

    @Test
    fun GIVEN_no_selected_file_WHEN_ui_state_is_created_THEN_message_asks_for_a_file() {
        val uiState = factory.create(PreviewUiStateFixtures.state(target = null))

        assertIs<PreviewContent.Empty>(uiState.content)
        assertTrue(uiState.content.text.contains("@Preview"))
    }

    @Test
    fun GIVEN_file_that_is_still_being_scanned_WHEN_ui_state_is_created_THEN_loading_names_the_file() {
        val target = PreviewUiStateFixtures.target(scan = PreviewScan.Pending)

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Loading>(uiState.content)
        assertTrue(uiState.content.text.contains(PreviewUiStateFixtures.FILE_NAME))
    }

    @Test
    fun GIVEN_file_without_previews_WHEN_ui_state_is_created_THEN_message_names_the_file() {
        val target = PreviewUiStateFixtures.target(scan = PreviewScan.NoPreviews)

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Empty>(uiState.content)
        assertTrue(uiState.content.text.contains(PreviewUiStateFixtures.FILE_NAME))
    }

    @Test
    fun GIVEN_previews_of_another_platform_WHEN_ui_state_is_created_THEN_message_names_the_source_sets() {
        val target = PreviewUiStateFixtures.target(scan = PreviewScan.UnsupportedSourceSet("jvmMain"))

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Empty>(uiState.content)
        assertTrue(uiState.content.text.contains("jvmMain"))
        assertTrue(uiState.content.text.contains("jsMain"))
    }

    @Test
    fun GIVEN_no_module_that_can_render_WHEN_ui_state_is_created_THEN_failure_carries_the_reason() {
        val target = PreviewUiStateFixtures.target(host = PreviewHostResolution.NotFound("No Kotlin/JS module"))

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Failure>(uiState.content)
        assertTrue(uiState.content.text.contains("No Kotlin/JS module"))
    }

    @Test
    fun GIVEN_failed_server_WHEN_ui_state_is_created_THEN_failure_offers_a_retry() {
        val serverState = DevServerState.Failed("Port 8085 is taken")

        val uiState = factory.create(PreviewUiStateFixtures.state(serverState = serverState))

        assertIs<PreviewContent.Failure>(uiState.content)
        assertTrue(uiState.content.text.contains("Port 8085 is taken"))
        assertTrue(uiState.content.text.contains("Refresh Preview"))
    }

    @Test
    fun GIVEN_page_the_browser_could_not_render_WHEN_ui_state_is_created_THEN_failure_offers_a_retry() {
        val state = PreviewUiStateFixtures.state(
            previewUrl = PreviewUiStateFixtures.PREVIEW_URL,
            serverState = PreviewUiStateFixtures.running,
            pageState = PageState.Failed("ERR_CONNECTION_REFUSED")
        )

        val uiState = factory.create(state)

        assertIs<PreviewContent.Failure>(uiState.content)
        assertTrue(uiState.content.text.contains("ERR_CONNECTION_REFUSED"))
        assertTrue(uiState.content.text.contains("Refresh Preview"))
    }

    /** A live reload can still bring the page back, so the browser keeps the address it failed on. */
    @Test
    fun GIVEN_page_the_browser_could_not_render_WHEN_ui_state_is_created_THEN_the_url_is_still_loaded() {
        val state = PreviewUiStateFixtures.state(
            previewUrl = PreviewUiStateFixtures.PREVIEW_URL,
            serverState = PreviewUiStateFixtures.running,
            pageState = PageState.Failed("ERR_CONNECTION_REFUSED")
        )

        val uiState = factory.create(state)

        assertEquals(PreviewUiStateFixtures.PREVIEW_URL, uiState.pageUrl)
    }

    @Test
    fun GIVEN_a_message_WHEN_ui_state_is_created_THEN_the_footer_says_nothing() {
        val uiState = factory.create(PreviewUiStateFixtures.state(target = null))

        assertEquals("", uiState.statusText)
    }

    @Test
    fun GIVEN_starting_server_and_no_page_WHEN_ui_state_is_created_THEN_loading_names_the_task() {
        val serverState = DevServerState.Starting(PreviewUiStateFixtures.host)

        val uiState = factory.create(PreviewUiStateFixtures.state(serverState = serverState))

        assertIs<PreviewContent.Loading>(uiState.content)
        assertTrue(uiState.content.text.contains(PreviewUiStateFixtures.host.kind.startTask))
        assertTrue(uiState.statusText.contains(PreviewUiStateFixtures.host.displayName))
    }

    @Test
    fun GIVEN_page_that_has_not_loaded_yet_WHEN_ui_state_is_created_THEN_it_is_loaded_but_not_shown() {
        val state = PreviewUiStateFixtures.state(
            previewUrl = PreviewUiStateFixtures.PREVIEW_URL,
            serverState = PreviewUiStateFixtures.running
        )

        val uiState = factory.create(state)

        assertIs<PreviewContent.Loading>(uiState.content)
        assertEquals(PreviewUiStateFixtures.PREVIEW_URL, uiState.pageUrl)
    }

    @Test
    fun GIVEN_loaded_page_WHEN_ui_state_is_created_THEN_content_is_the_page_and_the_footer_is_its_url() {
        val uiState = factory.create(PreviewUiStateFixtures.shownPageState())

        assertEquals(PreviewContent.Page(PreviewUiStateFixtures.PREVIEW_URL), uiState.content)
        assertEquals(PreviewUiStateFixtures.PREVIEW_URL, uiState.statusText)
    }

    @Test
    fun GIVEN_up_to_date_sources_WHEN_ui_state_is_created_THEN_there_is_no_banner() {
        val uiState = factory.create(PreviewUiStateFixtures.shownPageState())

        assertNull(uiState.banner)
    }

    @Test
    fun GIVEN_changed_sources_WHEN_ui_state_is_created_THEN_banner_reports_the_rebuild() {
        val sourceState = SourceState.Changed(
            changedFileName = PreviewUiStateFixtures.FILE_NAME,
            changedAt = Instant.parse("2026-09-07T10:15:30Z")
        )

        val uiState = factory.create(PreviewUiStateFixtures.shownPageState(sourceState))

        assertIs<PreviewBanner.Rebuilding>(uiState.banner)
        assertTrue(uiState.banner.text.contains(PreviewUiStateFixtures.FILE_NAME))
    }

    @Test
    fun GIVEN_reloaded_page_WHEN_ui_state_is_created_THEN_banner_reports_fresh_sources() {
        val sourceState = SourceState.Reloaded(reloadedAt = Instant.parse("2026-09-07T10:15:30Z"))

        val uiState = factory.create(PreviewUiStateFixtures.shownPageState(sourceState))

        assertIs<PreviewBanner.Reloaded>(uiState.banner)
        assertTrue(uiState.banner.text.contains("fresh sources"))
    }

    @Test
    fun GIVEN_a_message_state_WHEN_ui_state_is_created_THEN_the_text_is_plain_and_not_html() {
        val target = PreviewUiStateFixtures.target(host = PreviewHostResolution.NotFound("No Kotlin/JS module"))

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Failure>(uiState.content)
        assertTrue(uiState.content.text.none { symbol -> symbol == '<' })
    }
}
