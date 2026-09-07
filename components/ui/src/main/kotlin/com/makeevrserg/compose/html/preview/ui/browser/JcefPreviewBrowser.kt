package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.makeevrserg.compose.html.preview.feature.PageLoad
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

class JcefPreviewBrowser(
    private val jbCefBrowser: JBCefBrowser
) : PreviewBrowser {
    override val component: JComponent = jbCefBrowser.component

    override val isDevToolsSupported: Boolean = true

    /** The panel may ask for a page while the tool window is closing the browser down. */
    override fun load(url: String) {
        if (jbCefBrowser.isDisposed) return
        jbCefBrowser.loadURL(url)
    }

    override fun reload() = jbCefBrowser.cefBrowser.reload()

    override fun openDevTools() = jbCefBrowser.openDevtools()

    /**
     * The tool window may dispose the browser before the collector is cancelled; a disposed client has
     * no handlers.
     *
     * CEF splits the two ways a page can fail: it reports a transport error, a refused connection for
     * example, through `onLoadError` and then ends the load without a status, while an HTTP error page
     * is a successful load carrying the status of the server. Both are failures here, and a load that
     * ended without a status is not reported twice.
     */
    override fun loads(): Flow<PageLoad> = callbackFlow {
        val loadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String?,
                failedUrl: String?
            ) {
                if (!frame.isMain) return
                // A load replaced by the next one, which is what a fast file switch does
                if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED) return
                trySend(PageLoad.Failed(errorText?.takeIf(String::isNotBlank) ?: errorCode.name))
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (!frame.isMain) return
                if (!browser.url.orEmpty().startsWith(HTTP_SCHEME)) return
                when {
                    httpStatusCode == NO_STATUS -> Unit
                    httpStatusCode >= FIRST_ERROR_STATUS ->
                        trySend(PageLoad.Failed("The dev server answered HTTP $httpStatusCode"))
                    else -> trySend(PageLoad.Succeeded)
                }
            }
        }
        jbCefBrowser.jbCefClient.addLoadHandler(loadHandler, jbCefBrowser.cefBrowser)
        awaitClose {
            if (jbCefBrowser.isDisposed) return@awaitClose
            jbCefBrowser.jbCefClient.removeLoadHandler(loadHandler, jbCefBrowser.cefBrowser)
        }
    }

    override fun dispose() = Disposer.dispose(jbCefBrowser)

    private companion object {
        const val HTTP_SCHEME = "http"
        const val NO_STATUS = 0
        const val FIRST_ERROR_STATUS = 400
    }
}
