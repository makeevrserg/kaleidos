package com.makeevrserg.compose.html.preview.ui

import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Wording of the tool window for every state, kept apart from the Swing plumbing of [PreviewPanel]. */
class PreviewStateTexts {

    private fun Instant.toClockText(): String = TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))

    fun changedBanner(sourceState: SourceState.Changed): String {
        return "Sources changed (${sourceState.changedFileName}, ${sourceState.changedAt.toClockText()}). " +
            "Rebuilding, the page reloads when the dev server is done…"
    }

    fun reloadedBanner(sourceState: SourceState.Reloaded): String {
        return "Reloaded with fresh sources at ${sourceState.reloadedAt.toClockText()}"
    }

    fun status(serverState: DevServerState, previewUrl: String?): String {
        return when (serverState) {
            DevServerState.Stopped -> "Dev server is stopped"
            is DevServerState.Starting ->
                "Starting ${serverState.host.kind.startTask} in ${serverState.host.displayName}, waiting for the server…"
            is DevServerState.Running ->
                previewUrl ?: "Dev server of ${serverState.host.displayName} is running at ${serverState.baseUrl}"
            is DevServerState.Failed -> serverState.reason
        }
    }

    /** Text of the message card; null when the file has previews and a module that can render them. */
    fun message(state: PreviewState): String? {
        val target = state.target
        val host = target?.host
        val serverState = state.serverState
        return when {
            target == null -> "Open a Kotlin file with @Preview functions"
            target.previews.isEmpty() -> "No @Preview functions in ${target.fileName}"
            host is PreviewHostResolution.NotFound ->
                "<html><center>Cannot find the preview module.<br>${host.reason}</center></html>"
            serverState is DevServerState.Failed ->
                "<html><center>Dev server failed.<br>${serverState.reason}<br><br>" +
                    "Press Refresh Preview to try again.</center></html>"
            else -> null
        }
    }

    /** What the spinner waits for while there is no page for the target yet. */
    fun loading(state: PreviewState): String {
        val host = (state.target?.host as? PreviewHostResolution.Found)?.host
        val starting = state.serverState as? DevServerState.Starting
        return when {
            host == null -> "Looking for the preview module…"
            starting != null && starting.host == host -> "Starting ${host.kind.startTask} in ${host.displayName}…"
            else -> "Connecting to the dev server of ${host.displayName}…"
        }
    }

    private companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
