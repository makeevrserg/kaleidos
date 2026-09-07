package com.makeevrserg.compose.html.preview.feature

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract between the preview UI and its logic. All intents are safe to call from any thread.
 * The preview follows the selected editor file and renders only while the tool window is visible.
 */
interface PreviewStore {
    val state: StateFlow<PreviewState>

    /**
     * The selected editor file changed or its previews were rescanned; null when no file is selected.
     * A newly selected file is reported twice: once with [PreviewScan.Pending] the moment the editor
     * switches, and again with the result of its scan.
     */
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

    /** The browser finished loading the current page, whether it can show it or not. */
    fun onPageLoaded(load: PageLoad)
}
