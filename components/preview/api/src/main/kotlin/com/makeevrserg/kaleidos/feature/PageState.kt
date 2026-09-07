package com.makeevrserg.kaleidos.feature

/**
 * What the embedded browser has done with [PreviewState.previewUrl]. Starts over whenever that URL
 * changes, so a page loaded for one file is never reported as shown for the next one.
 */
sealed interface PageState {

    /** The page has been requested and is not on screen yet. */
    data object Loading : PageState

    data object Shown : PageState

    /** The address answered with a page the plugin did not generate. */
    data object Foreign : PageState

    /** @param reason why the browser could not render the page, worded for the user */
    data class Failed(
        val reason: String
    ) : PageState
}
