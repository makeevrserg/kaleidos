package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser

class PreviewBrowserFactory {

    val isJcefSupported: Boolean
        get() = JBCefApp.isSupported()

    /**
     * Must be called on the event dispatch thread: JCEF components are Swing components.
     *
     * No placeholder page is loaded on purpose. [JBCefBrowser] defers loads requested before the
     * native browser exists and keeps only one of them, so a placeholder followed by the preview URL
     * could leave the tab on the placeholder forever.
     */
    fun create(): PreviewBrowser {
        if (!isJcefSupported) return UnsupportedPreviewBrowser()
        return JcefPreviewBrowser(JBCefBrowser.createBuilder().build())
    }
}
