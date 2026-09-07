package com.makeevrserg.kaleidos.ui.state

/** Strip above the page that reports its freshness. Absent while the page matches the sources. */
sealed interface PreviewBanner {
    val text: String

    /** A source changed and the dev server has not reloaded the page yet. */
    data class Rebuilding(override val text: String) : PreviewBanner

    data class Reloaded(override val text: String) : PreviewBanner
}
