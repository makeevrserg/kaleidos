package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.core.TestCoroutineFeature
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.BUTTON_FILE
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.CARD_FILE
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.hostFound
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.previewFunction
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.previewHost
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.running
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.target
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.url.PreviewUrlFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewFeatureTest {
    private val devServerController = FakeDevServerController()

    private val hostLocator = FakePreviewHostLocator()

    private val toolWindowPresenter = FakePreviewToolWindowPresenter()

    private val notifier = FakePreviewNotifier()

    private fun TestScope.createFeature(): PreviewFeature {
        return PreviewFeature(
            devServerController = devServerController,
            hostLocator = hostLocator,
            toolWindowPresenter = toolWindowPresenter,
            previewNotifier = notifier,
            reducer = PreviewStateReducer(
                clock = Clock.fixed(Instant.parse("2026-09-06T10:00:00Z"), ZoneOffset.UTC),
                previewUrlFactory = PreviewUrlFactory()
            ),
            coroutineFeature = TestCoroutineFeature(backgroundScope)
        )
    }

    /** The tool window is open and the card file with a known host is selected. */
    private fun TestScope.createVisibleFeatureWithCardFile(): PreviewFeature {
        hostLocator.resolutions[CARD_FILE] = hostFound
        val feature = createFeature()
        feature.onToolWindowVisibilityChanged(isVisible = true)
        feature.onFileSelected(target())
        runCurrent()
        return feature
    }

    @Test
    fun GIVEN_tool_window_hidden_WHEN_file_with_previews_selected_THEN_nothing_is_looked_up_or_started() = runTest {
        hostLocator.resolutions[CARD_FILE] = hostFound
        val feature = createFeature()

        feature.onFileSelected(target())
        runCurrent()

        assertTrue(hostLocator.locatedFiles.isEmpty())
        assertTrue(devServerController.runningRequests.isEmpty())
        assertEquals(target(), feature.state.value.target)
    }

    @Test
    fun GIVEN_tool_window_visible_WHEN_file_with_previews_selected_THEN_host_found_and_server_ensured() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        assertEquals(listOf(CARD_FILE), hostLocator.locatedFiles)
        assertEquals(listOf(RunningRequest(previewHost, false)), devServerController.runningRequests)
        assertEquals(hostFound, feature.state.value.target?.host)
    }

    @Test
    fun GIVEN_tool_window_visible_WHEN_file_without_previews_selected_THEN_no_server_contact() = runTest {
        val feature = createFeature()
        feature.onToolWindowVisibilityChanged(isVisible = true)

        feature.onFileSelected(target(previews = emptyList()))
        runCurrent()

        assertTrue(hostLocator.locatedFiles.isEmpty())
        assertTrue(devServerController.runningRequests.isEmpty())
    }

    @Test
    fun GIVEN_host_found_WHEN_same_file_rescanned_THEN_host_is_not_looked_up_again() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        feature.onFileSelected(target(previews = listOf(previewFunction("CardPreview"), previewFunction("Other"))))
        runCurrent()

        assertEquals(1, hostLocator.locatedFiles.size)
        assertEquals(2, devServerController.runningRequests.size)
    }

    @Test
    fun GIVEN_host_not_found_WHEN_file_selected_THEN_user_notified_once_for_the_same_reason() = runTest {
        hostLocator.resolutions[CARD_FILE] = PreviewHostResolution.NotFound("No preview module")
        val feature = createFeature()
        feature.onToolWindowVisibilityChanged(isVisible = true)

        feature.onFileSelected(target())
        runCurrent()
        feature.onFileSelected(target())
        runCurrent()

        assertEquals(listOf("No preview module"), notifier.errors)
        assertEquals(2, hostLocator.locatedFiles.size)
        assertTrue(devServerController.runningRequests.isEmpty())
    }

    @Test
    fun GIVEN_host_not_found_earlier_WHEN_lookup_later_succeeds_THEN_server_ensured() = runTest {
        hostLocator.resolutions[CARD_FILE] = PreviewHostResolution.NotFound("Gradle not imported")
        val feature = createFeature()
        feature.onToolWindowVisibilityChanged(isVisible = true)
        feature.onFileSelected(target())
        runCurrent()

        hostLocator.resolutions[CARD_FILE] = hostFound
        feature.onReconnect()
        runCurrent()

        assertEquals(listOf(RunningRequest(previewHost, true)), devServerController.runningRequests)
        assertEquals(hostFound, feature.state.value.target?.host)
    }

    @Test
    fun GIVEN_file_selected_WHEN_gutter_icon_pressed_THEN_tool_window_shown_and_launch_retried() = runTest {
        hostLocator.resolutions[CARD_FILE] = hostFound
        val feature = createFeature()
        feature.onToolWindowVisibilityChanged(isVisible = true)

        feature.onFocusPreview(previewFunction("CardPreview"))
        runCurrent()

        assertEquals(1, toolWindowPresenter.showCount)
        assertEquals(listOf(RunningRequest(previewHost, true)), devServerController.runningRequests)
        assertEquals("app.CardPreview", feature.state.value.target?.focusedFqn)
    }

    @Test
    fun GIVEN_gutter_icon_of_unselected_file_WHEN_pressed_THEN_that_file_becomes_the_target() = runTest {
        hostLocator.resolutions[BUTTON_FILE] = hostFound
        val feature = createVisibleFeatureWithCardFile()

        feature.onFocusPreview(previewFunction("ButtonPreview", BUTTON_FILE))
        runCurrent()

        assertEquals(BUTTON_FILE, feature.state.value.target?.filePath)
        assertEquals(listOf(CARD_FILE, BUTTON_FILE), hostLocator.locatedFiles)
    }

    @Test
    fun GIVEN_hidden_tool_window_WHEN_it_becomes_visible_THEN_server_ensured_with_retry() = runTest {
        hostLocator.resolutions[CARD_FILE] = hostFound
        val feature = createFeature()
        feature.onFileSelected(target())
        runCurrent()

        feature.onToolWindowVisibilityChanged(isVisible = true)
        runCurrent()

        assertEquals(listOf(RunningRequest(previewHost, true)), devServerController.runningRequests)
    }

    @Test
    fun GIVEN_visible_tool_window_WHEN_visibility_reported_again_THEN_no_new_request() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        feature.onToolWindowVisibilityChanged(isVisible = true)
        runCurrent()

        assertEquals(1, devServerController.runningRequests.size)
    }

    @Test
    fun GIVEN_found_host_WHEN_reconnect_THEN_host_looked_up_again_and_launch_retried() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        feature.onReconnect()
        runCurrent()

        assertEquals(2, hostLocator.locatedFiles.size)
        assertEquals(RunningRequest(previewHost, true), devServerController.runningRequests.last())
    }

    @Test
    fun GIVEN_found_host_WHEN_restart_requested_THEN_controller_restarts_that_host() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        feature.onRestartServer()
        runCurrent()

        assertEquals(listOf(previewHost), devServerController.restartedHosts)
    }

    @Test
    fun GIVEN_no_file_WHEN_restart_requested_THEN_nothing_happens() = runTest {
        val feature = createFeature()

        feature.onRestartServer()
        runCurrent()

        assertTrue(devServerController.restartedHosts.isEmpty())
    }

    @Test
    fun GIVEN_running_server_WHEN_stop_requested_THEN_controller_stops() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        feature.onStopServer()
        runCurrent()

        assertEquals(1, devServerController.stopCount)
    }

    @Test
    fun GIVEN_server_running_for_target_module_WHEN_state_observed_THEN_preview_url_available() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        devServerController.mutableState.value = running
        runCurrent()

        assertEquals("http://localhost:8085/?preview=app.CardPreview", feature.state.value.previewUrl)
        assertEquals(running, feature.state.value.serverState)
    }

    @Test
    fun GIVEN_server_launch_fails_WHEN_state_observed_THEN_user_notified_with_reason() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        devServerController.mutableState.value = DevServerState.Failed("Gradle task failed")
        runCurrent()

        assertEquals(listOf("Gradle task failed"), notifier.errors)
        assertIs<DevServerState.Failed>(feature.state.value.serverState)
    }

    @Test
    fun GIVEN_page_shown_WHEN_source_changes_and_page_reloads_THEN_freshness_follows() = runTest {
        val feature = createVisibleFeatureWithCardFile()

        feature.onSourceChanged("CardPreview.kt")
        val changed = feature.state.value.sourceState
        feature.onPageLoaded(isReload = true)
        val reloaded = feature.state.value.sourceState

        assertIs<SourceState.Changed>(changed)
        assertEquals("CardPreview.kt", changed.changedFileName)
        assertIs<SourceState.Reloaded>(reloaded)
    }
}
