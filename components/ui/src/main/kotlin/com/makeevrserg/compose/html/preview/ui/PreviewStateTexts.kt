package com.makeevrserg.compose.html.preview.ui

import com.makeevrserg.compose.html.preview.feature.PageState
import com.makeevrserg.compose.html.preview.feature.PreviewScan
import com.makeevrserg.compose.html.preview.feature.PreviewSourceSetCatalog
import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Wording of the tool window for every state, kept apart from the composables that render it. */
class PreviewStateTexts {

    val noSelectedFile: String = "Open a Kotlin file with @Preview functions"

    /** Shown while the browser loads a page the dev server already serves. */
    val pageLoading: String = "Loading preview…"

    /** Takes the place of the page on IDE runtimes without the embedded browser. */
    val unsupportedBrowser: String = "The embedded browser (JCEF) is not available in this IDE runtime.\n" +
        "Use Open in Browser to view the preview."

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

    fun scanning(fileName: String): String = "Looking for @Preview functions in $fileName…"

    fun noPreviews(fileName: String): String = "No @Preview functions in $fileName"

    fun unsupportedSourceSet(fileName: String, scan: PreviewScan.UnsupportedSourceSet): String {
        val supported = PreviewSourceSetCatalog.previewSourceSets.joinToString(SOURCE_SET_SEPARATOR)
        return "The @Preview functions of $fileName are in ${scan.sourceSetName}, " +
            "which is not compiled to Kotlin/JS.\n" +
            "Only previews from $supported end up on the page this plugin renders."
    }

    fun hostNotFound(host: PreviewHostResolution.NotFound): String {
        return "Nothing in this project can render the preview.\n${host.reason}"
    }

    fun serverFailed(serverState: DevServerState.Failed): String {
        return """
            Dev server failed.
            ${serverState.reason}

            Press Refresh Preview to try again.
        """.trimIndent()
    }

    fun pageFailed(pageState: PageState.Failed): String {
        return """
            The preview page could not be rendered.
            ${pageState.reason}

            Press Refresh Preview to try again.
        """.trimIndent()
    }

    /** What the spinner waits for while the module of the target has no page for it yet. */
    fun connecting(state: PreviewState): String {
        val host = (state.target?.host as? PreviewHostResolution.Found)?.host
        val starting = state.serverState as? DevServerState.Starting
        return when {
            host == null -> "Looking for the module that can render the previews…"
            starting != null && starting.host == host -> "Starting ${host.kind.startTask} in ${host.displayName}…"
            else -> "Connecting to the dev server of ${host.displayName}…"
        }
    }

    private companion object {
        const val SOURCE_SET_SEPARATOR = ", "
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
