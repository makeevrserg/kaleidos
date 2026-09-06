package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.BUTTON_FILE
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.CARD_FILE
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.hostFound
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.initialState
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.otherHost
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.previewFunction
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.running
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.target
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.url.PreviewUrlFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreviewStateReducerTest {
    private val now: Instant = Instant.parse("2026-09-06T10:00:00Z")

    private val reducer = PreviewStateReducer(
        clock = Clock.fixed(now, ZoneOffset.UTC),
        previewUrlFactory = PreviewUrlFactory()
    )

    /** A file with a found host whose server is running: the state in which a page is shown. */
    private fun servedState(): PreviewState {
        val selected = reducer.select(initialState(), target())
        val withHost = reducer.attachHost(selected, CARD_FILE, hostFound)
        return reducer.setServerState(withHost, running)
    }

    @Test
    fun GIVEN_no_target_WHEN_select_file_THEN_target_set_without_host_and_url() {
        val state = reducer.select(initialState(), target())

        assertEquals(target(), state.target)
        assertNull(state.previewUrl)
        assertEquals(SourceState.UpToDate, state.sourceState)
    }

    @Test
    fun GIVEN_served_file_WHEN_same_file_is_rescanned_THEN_host_freshness_and_url_are_kept() {
        val served = reducer.markChanged(servedState(), "CardPreview.kt")
        val rescanned = target(previews = listOf(previewFunction("CardPreview"), previewFunction("DarkCardPreview")))

        val state = reducer.select(served, rescanned)

        assertEquals(hostFound, state.target?.host)
        assertEquals(2, state.target?.previews?.size)
        assertEquals(SourceState.Changed("CardPreview.kt", now), state.sourceState)
        assertEquals(
            "http://localhost:8085/compose-html-preview.html?preview=app.CardPreview,app.DarkCardPreview",
            state.previewUrl
        )
    }

    @Test
    fun GIVEN_served_file_WHEN_another_file_is_selected_THEN_host_freshness_and_url_start_over() {
        val served = reducer.markChanged(servedState(), "CardPreview.kt")

        val state = reducer.select(served, target(filePath = BUTTON_FILE))

        assertNull(state.target?.host)
        assertNull(state.previewUrl)
        assertEquals(SourceState.UpToDate, state.sourceState)
    }

    @Test
    fun GIVEN_served_file_WHEN_no_file_is_selected_THEN_target_and_url_cleared() {
        val state = reducer.select(servedState(), null)

        assertNull(state.target)
        assertNull(state.previewUrl)
    }

    @Test
    fun GIVEN_served_file_WHEN_focus_on_its_preview_THEN_url_gets_fragment() {
        val state = reducer.focus(servedState(), previewFunction("CardPreview"))

        assertEquals("app.CardPreview", state.target?.focusedFqn)
        assertEquals(
            "http://localhost:8085/compose-html-preview.html?preview=app.CardPreview#app.CardPreview",
            state.previewUrl
        )
    }

    @Test
    fun GIVEN_served_file_WHEN_focus_on_preview_of_another_file_THEN_temporary_target_with_that_preview() {
        val buttonPreview = previewFunction("ButtonPreview", BUTTON_FILE)

        val state = reducer.focus(servedState(), buttonPreview)

        assertEquals(target(BUTTON_FILE, listOf(buttonPreview), focusedFqn = buttonPreview.fqn), state.target)
        assertNull(state.previewUrl)
    }

    @Test
    fun GIVEN_running_server_of_the_files_module_WHEN_host_attached_THEN_url_derived() {
        val selected = reducer.setServerState(reducer.select(initialState(), target()), running)

        val state = reducer.attachHost(selected, CARD_FILE, hostFound)

        assertEquals("http://localhost:8085/compose-html-preview.html?preview=app.CardPreview", state.previewUrl)
    }

    @Test
    fun GIVEN_target_moved_to_another_file_WHEN_host_of_old_file_arrives_THEN_ignored() {
        val selected = reducer.select(initialState(), target(filePath = BUTTON_FILE))

        val state = reducer.attachHost(selected, CARD_FILE, hostFound)

        assertEquals(selected, state)
    }

    @Test
    fun GIVEN_host_not_found_WHEN_server_running_THEN_no_url() {
        val selected = reducer.select(initialState(), target())
        val notFound = reducer.attachHost(selected, CARD_FILE, PreviewHostResolution.NotFound("no module"))

        val state = reducer.setServerState(notFound, running)

        assertNull(state.previewUrl)
    }

    @Test
    fun GIVEN_server_of_another_module_running_WHEN_server_state_set_THEN_no_url() {
        val withHost = reducer.attachHost(reducer.select(initialState(), target()), CARD_FILE, hostFound)

        val state = reducer.setServerState(withHost, DevServerState.Running(otherHost, "http://localhost:8080"))

        assertNull(state.previewUrl)
    }

    @Test
    fun GIVEN_file_without_previews_WHEN_served_THEN_no_url() {
        val selected = reducer.select(initialState(), target(previews = emptyList()))
        val withHost = reducer.attachHost(selected, CARD_FILE, hostFound)

        val state = reducer.setServerState(withHost, running)

        assertNull(state.previewUrl)
    }

    @Test
    fun GIVEN_served_file_WHEN_server_stops_THEN_url_dropped() {
        val state = reducer.setServerState(servedState(), DevServerState.Stopped)

        assertNull(state.previewUrl)
        assertEquals(DevServerState.Stopped, state.serverState)
    }

    @Test
    fun GIVEN_up_to_date_page_WHEN_sources_change_twice_THEN_first_change_is_kept() {
        val changed = reducer.markChanged(servedState(), "CardPreview.kt")

        val state = reducer.markChanged(changed, "ButtonPreview.kt")

        assertEquals(SourceState.Changed("CardPreview.kt", now), state.sourceState)
    }

    @Test
    fun GIVEN_up_to_date_page_WHEN_first_load_finishes_THEN_still_up_to_date() {
        val state = reducer.markLoaded(servedState(), isReload = false)

        assertEquals(SourceState.UpToDate, state.sourceState)
    }

    @Test
    fun GIVEN_changed_sources_WHEN_page_loads_THEN_reloaded() {
        val changed = reducer.markChanged(servedState(), "CardPreview.kt")

        val state = reducer.markLoaded(changed, isReload = false)

        assertEquals(SourceState.Reloaded(now), state.sourceState)
    }

    @Test
    fun GIVEN_up_to_date_page_WHEN_live_reload_happens_THEN_reloaded() {
        val state = reducer.markLoaded(servedState(), isReload = true)

        assertEquals(SourceState.Reloaded(now), state.sourceState)
    }
}
