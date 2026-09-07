package com.makeevrserg.kaleidos.ui

import com.makeevrserg.kaleidos.feature.SourceState
import com.makeevrserg.kaleidos.server.DevServerState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Wording of the strips around the page: the freshness banner above it and the footer below it. */
class PreviewStatusTexts {

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

    private companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
