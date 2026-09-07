package com.makeevrserg.kaleidos.feature

import java.time.Instant

/**
 * Freshness of a rendered preview relative to the Kotlin sources of the project.
 */
sealed interface SourceState {
    data object UpToDate : SourceState

    /** A Kotlin source changed after the page was loaded; the dev server has not reloaded the page yet. */
    data class Changed(
        val changedFileName: String,
        val changedAt: Instant
    ) : SourceState

    /** The page was reloaded after a source change. */
    data class Reloaded(
        val reloadedAt: Instant
    ) : SourceState
}
