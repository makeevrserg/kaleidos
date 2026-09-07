package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.openapi.Disposable
import com.makeevrserg.compose.html.preview.feature.PageLoad
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

    fun load(url: String)

    /** Drops whatever is on screen, so the page of one file is never seen while another is selected. */
    fun clear()

    fun reload()

    fun openDevTools()

    /**
     * Finished main-frame loads of preview pages, live reloads included. Cold: the browser is observed
     * only while collected. May emit off the event dispatch thread.
     */
    fun loads(): Flow<PageLoad>
}
