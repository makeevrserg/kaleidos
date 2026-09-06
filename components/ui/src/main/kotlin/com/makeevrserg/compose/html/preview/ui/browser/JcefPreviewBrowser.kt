package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

class JcefPreviewBrowser(
    private val jbCefBrowser: JBCefBrowser
) : PreviewBrowser {
    override val component: JComponent = jbCefBrowser.component

    override val isDevToolsSupported: Boolean = true

    override fun load(url: String) = jbCefBrowser.loadURL(url)

    override fun reload() = jbCefBrowser.cefBrowser.reload()

    override fun openDevTools() = jbCefBrowser.openDevtools()

    override fun addPageLoadListener(listener: PageLoadListener) {
        val loadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) listener.onPageLoaded(browser.url.orEmpty())
            }
        }
        jbCefBrowser.jbCefClient.addLoadHandler(loadHandler, jbCefBrowser.cefBrowser)
    }

    override fun dispose() = Disposer.dispose(jbCefBrowser)
}
