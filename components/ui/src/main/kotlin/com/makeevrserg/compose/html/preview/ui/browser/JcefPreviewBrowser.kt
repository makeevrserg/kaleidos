package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

class JcefPreviewBrowser(
    private val jbCefBrowser: JBCefBrowser
) : PreviewBrowser {
    override val component: JComponent = jbCefBrowser.component

    override val isDevToolsSupported: Boolean = true

    /** The panel may ask for a blank page while the tool window is closing the browser down. */
    override fun load(url: String) {
        if (jbCefBrowser.isDisposed) return
        jbCefBrowser.loadURL(url)
    }

    override fun reload() = jbCefBrowser.cefBrowser.reload()

    override fun openDevTools() = jbCefBrowser.openDevtools()

    /** The tool window may dispose the browser before the collector is cancelled; a disposed client has no handlers. */
    override fun pageLoads(): Flow<String> = callbackFlow {
        val loadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) trySend(browser.url.orEmpty())
            }
        }
        jbCefBrowser.jbCefClient.addLoadHandler(loadHandler, jbCefBrowser.cefBrowser)
        awaitClose {
            if (jbCefBrowser.isDisposed) return@awaitClose
            jbCefBrowser.jbCefClient.removeLoadHandler(loadHandler, jbCefBrowser.cefBrowser)
        }
    }

    override fun dispose() = Disposer.dispose(jbCefBrowser)
}
