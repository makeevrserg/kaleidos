package com.makeevrserg.kaleidos.ui.browser.api

import com.intellij.openapi.Disposable
import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import kotlinx.coroutines.flow.Flow
import javax.swing.JComponent

/**
 * Rendering surface of one preview tab. Abstracts JCEF so the panel keeps working, in a degraded
 * mode, on IDE runtimes without the embedded browser.
 */
interface PreviewBrowser : Disposable {

    /** Native surface hosted by the panel; null when this runtime cannot render pages at all. */
    val component: JComponent?

    val isDevToolsSupported: Boolean

    fun reload()

    fun openDevTools()

    /**
     * Observes before loading the first URL, then reports main-frame loads, including live reloads.
     * A null URL clears the page. One collection belongs to the panel for its lifetime; cancellation
     * removes its handlers and pending title checks. Collect on the browser's UI dispatcher.
     */
    fun observePages(urls: Flow<String?>): Flow<PageLoad>
}
