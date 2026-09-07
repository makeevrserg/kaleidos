package com.makeevrserg.kaleidos.ui.state

import com.makeevrserg.kaleidos.feature.PageState
import com.makeevrserg.kaleidos.feature.PreviewScan
import com.makeevrserg.kaleidos.feature.SourceState
import com.makeevrserg.kaleidos.host.PreviewHostResolution
import com.makeevrserg.kaleidos.server.DevServerState
import com.makeevrserg.kaleidos.ui.PreviewMessageTexts
import com.makeevrserg.kaleidos.ui.PreviewStatusTexts
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreviewUiStateFactoryTest {

    private val messageTexts = PreviewMessageTexts()

    private val factory = PreviewUiStateFactory(
        statusTexts = PreviewStatusTexts(),
        messageTexts = messageTexts
    )

    @Test
    fun GIVEN_no_selected_file_WHEN_ui_state_is_created_THEN_message_asks_for_a_file() {
        val uiState = factory.create(PreviewUiStateFixtures.state(target = null))

        assertIs<PreviewContent.Empty>(uiState.content)
        assertTrue(uiState.content.message.description.contains("@Preview"))
    }

    @Test
    fun GIVEN_file_that_is_still_being_scanned_WHEN_ui_state_is_created_THEN_loading_names_the_file() {
        val target = PreviewUiStateFixtures.target(scan = PreviewScan.Pending)

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Loading>(uiState.content)
        assertTrue(uiState.content.message.description.contains(PreviewUiStateFixtures.FILE_NAME))
    }

    @Test
    fun GIVEN_file_without_previews_WHEN_ui_state_is_created_THEN_message_names_the_file() {
        val target = PreviewUiStateFixtures.target(scan = PreviewScan.NoPreviews)

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Empty>(uiState.content)
        assertTrue(uiState.content.message.title.contains(PreviewUiStateFixtures.FILE_NAME))
    }

    @Test
    fun GIVEN_previews_of_another_platform_WHEN_ui_state_is_created_THEN_message_names_the_source_sets() {
        val target = PreviewUiStateFixtures.target(scan = PreviewScan.UnsupportedSourceSet("jvmMain"))

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Empty>(uiState.content)
        assertTrue(uiState.content.message.title.contains(PreviewUiStateFixtures.FILE_NAME))
        assertTrue(uiState.content.message.description.contains("jvmMain"))
        assertTrue(uiState.content.message.description.contains("jsMain"))
    }

    @Test
    fun GIVEN_no_module_that_can_render_WHEN_ui_state_is_created_THEN_failure_carries_the_reason() {
        val target = PreviewUiStateFixtures.target(host = PreviewHostResolution.NotFound("No Kotlin/JS module"))

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertIs<PreviewContent.Failure>(uiState.content)
        assertEquals("No Kotlin/JS module", uiState.content.message.description)
    }

    @Test
    fun GIVEN_failed_server_WHEN_ui_state_is_created_THEN_failure_offers_a_retry() {
        val serverState = DevServerState.Failed("Port 8085 is taken")

        val uiState = factory.create(PreviewUiStateFixtures.state(serverState = serverState))

        assertIs<PreviewContent.Failure>(uiState.content)
        assertTrue(uiState.content.message.description.contains("Port 8085 is taken"))
        assertTrue(uiState.content.message.description.contains("Refresh Preview"))
    }

    @Test
    fun GIVEN_page_the_browser_could_not_load_WHEN_ui_state_is_created_THEN_failure_offers_a_retry() {
        val state = PreviewUiStateFixtures.pageState(PageState.Failed("ERR_CONNECTION_REFUSED"))

        val uiState = factory.create(state)

        assertIs<PreviewContent.Failure>(uiState.content)
        assertTrue(uiState.content.message.description.contains("ERR_CONNECTION_REFUSED"))
        assertTrue(uiState.content.message.description.contains("Refresh Preview"))
    }

    @Test
    fun GIVEN_dev_server_that_answered_with_another_page_WHEN_ui_state_is_created_THEN_failure_offers_a_restart() {
        val state = PreviewUiStateFixtures.pageState(PageState.Foreign)

        val uiState = factory.create(state)

        assertIs<PreviewContent.Failure>(uiState.content)
        assertTrue(uiState.content.message.description.contains(PreviewUiStateFixtures.host.displayName))
        assertTrue(uiState.content.message.description.contains("Restart Dev Server"))
    }

    /** The browser covers whatever is drawn where it sits, so a failure takes the address away from it. */
    @Test
    fun GIVEN_page_the_browser_could_not_load_WHEN_ui_state_is_created_THEN_the_browser_gives_up_the_url() {
        val state = PreviewUiStateFixtures.pageState(PageState.Failed("ERR_CONNECTION_REFUSED"))

        val uiState = factory.create(state)

        assertNull(uiState.pageUrl)
    }

    @Test
    fun GIVEN_dev_server_that_answered_with_another_page_WHEN_ui_state_is_created_THEN_the_browser_gives_up_the_url() {
        val uiState = factory.create(PreviewUiStateFixtures.pageState(PageState.Foreign))

        assertNull(uiState.pageUrl)
    }

    @Test
    fun GIVEN_a_message_WHEN_ui_state_is_created_THEN_the_footer_says_nothing() {
        val uiState = factory.create(PreviewUiStateFixtures.state(target = null))

        assertEquals("", uiState.statusText)
    }

    @Test
    fun GIVEN_host_not_looked_up_yet_WHEN_ui_state_is_created_THEN_loading_says_so() {
        val target = PreviewUiStateFixtures.target(host = null)

        val uiState = factory.create(PreviewUiStateFixtures.state(target = target))

        assertEquals(PreviewContent.Loading(messageTexts.lookingForHost), uiState.content)
    }

    @Test
    fun GIVEN_starting_server_and_no_page_WHEN_ui_state_is_created_THEN_loading_names_the_task() {
        val serverState = DevServerState.Starting(PreviewUiStateFixtures.host)

        val uiState = factory.create(PreviewUiStateFixtures.state(serverState = serverState))

        assertIs<PreviewContent.Loading>(uiState.content)
        assertTrue(uiState.content.message.description.contains(PreviewUiStateFixtures.host.kind.startTask))
        assertTrue(uiState.statusText.contains(PreviewUiStateFixtures.host.displayName))
    }

    /**
     * The browser is given the address at once and the message stays in its place until the page is
     * there: a browser that has room covers whatever is drawn where it sits.
     */
    @Test
    fun GIVEN_page_that_has_not_loaded_yet_WHEN_ui_state_is_created_THEN_it_is_loaded_behind_a_message() {
        val uiState = factory.create(PreviewUiStateFixtures.pageState(PageState.Loading))

        assertIs<PreviewContent.Loading>(uiState.content)
        assertTrue(uiState.content.message.description.contains(PreviewUiStateFixtures.host.displayName))
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
        assertTrue(uiState.content.message.description.none { symbol: Char -> symbol == '<' })
    }
}
