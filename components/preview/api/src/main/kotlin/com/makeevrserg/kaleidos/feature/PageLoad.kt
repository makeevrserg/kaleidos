package com.makeevrserg.kaleidos.feature

/** Outcome of one load of [PreviewState.previewUrl] reported by the browser. */
sealed interface PageLoad {

    data object Succeeded : PageLoad

    /**
     * The address answered, but with something other than the generated page. A dev server that was
     * already running when the plugin attached to it was built without that page, and a single page
     * application answers every path with the shell of the real site, so the browser shows nothing.
     */
    data object Foreign : PageLoad

    /** @param reason what went wrong, in the words of the browser or the dev server */
    data class Failed(
        val reason: String
    ) : PageLoad
}
