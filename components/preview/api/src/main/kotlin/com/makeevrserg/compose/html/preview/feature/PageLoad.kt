package com.makeevrserg.compose.html.preview.feature

/** Outcome of one load of [PreviewState.previewUrl] reported by the browser. */
sealed interface PageLoad {

    data object Succeeded : PageLoad

    /** @param reason what went wrong, in the words of the browser or the dev server */
    data class Failed(
        val reason: String
    ) : PageLoad
}
