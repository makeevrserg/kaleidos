package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.psi.PreviewFunction
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract between the preview UI and its logic. All intents are safe to call from any thread.
 * The preview follows the selected editor file and renders only while the tool window is visible.
 */
interface PreviewStore {
    val state: StateFlow<PreviewState>

    /** The selected editor file changed or its previews were rescanned; null when no file is selected. */
    fun onFileSelected(target: PreviewTarget?)

    /** Gutter icon: show the tool window and scroll the page to this preview. */
    fun onFocusPreview(previewFunction: PreviewFunction)

    fun onToolWindowVisibilityChanged(isVisible: Boolean)

    /** Re-checks the dev server for the current page, starting it when allowed. */
    fun onReconnect()

    fun onRestartServer()

    fun onStopServer()

    /** A Kotlin source of the project changed; the rendered page may be stale. */
    fun onSourceChanged(changedFileName: String)

    /**
     * The page finished loading.
     *
     * @param isReload false for the first load of a URL, true for every later load such as a live reload
     */
    fun onPageLoaded(isReload: Boolean)
}
