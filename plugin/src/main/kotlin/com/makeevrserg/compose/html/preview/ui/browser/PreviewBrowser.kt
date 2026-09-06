package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.openapi.Disposable
import javax.swing.JComponent

/**
 * Rendering surface of one preview tab. Abstracts JCEF so the panel keeps working, in a degraded
 * mode, on IDE runtimes without the embedded browser.
 */
interface PreviewBrowser : Disposable {
    val component: JComponent

    val isDevToolsSupported: Boolean

    fun load(url: String)

    fun reload()

    fun openDevTools()

    fun addPageLoadListener(listener: PageLoadListener)
}
