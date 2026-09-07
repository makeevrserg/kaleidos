package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.makeevrserg.compose.html.preview.feature.PageLoad
import com.makeevrserg.compose.html.preview.harness.PreviewPagePath
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import javax.swing.JComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

    override fun clear() = load(BLANK_PAGE)

    override fun reload() = jbCefBrowser.cefBrowser.reload()

    override fun openDevTools() = jbCefBrowser.openDevtools()

    /**
     * The tool window may dispose the browser before the collector is cancelled; a disposed client has
     * no handlers.
     *
     * A load that ends is not yet a preview: a Kobweb site answers every path with the shell of the real
     * site, so a dev server built without the generated page returns HTTP 200 and renders nothing at all.
     * The page says who it is by its title, and only a load that ends with that title counts as shown.
     * The title is dropped when a load starts, so the title of the page before it cannot vouch for the
     * next one.
     *
     * CEF splits the two ways a page can fail: it reports a transport error, a refused connection for
     * example, through `onLoadError` and then ends the load without a status, while an HTTP error page
     * is a successful load carrying the status of the server. Both are failures here, and a load that
     * ended without a status is not reported twice.
     */
    override fun loads(): Flow<PageLoad> = channelFlow {
        val title = MutableStateFlow<String?>(null)
        var identification: Job? = null
        val displayHandler = object : CefDisplayHandlerAdapter() {
            override fun onTitleChange(browser: CefBrowser?, newTitle: String?) {
                title.value = newTitle
            }
        }
        val loadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
                if (frame.isMain) title.value = null
            }

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
                if (httpStatusCode == NO_STATUS) return
                if (httpStatusCode >= FIRST_ERROR_STATUS) {
                    trySend(PageLoad.Failed("The dev server answered HTTP $httpStatusCode"))
                    return
                }
                identification?.cancel()
                identification = launch {
                    val identified = withTimeoutOrNull(IDENTIFICATION_TIMEOUT) {
                        title.first { current -> current == PreviewPagePath.TITLE }
                    }
                    send(if (identified == null) PageLoad.Foreign else PageLoad.Succeeded)
                }
            }
        }
        jbCefBrowser.jbCefClient.addLoadHandler(loadHandler, jbCefBrowser.cefBrowser)
        jbCefBrowser.jbCefClient.addDisplayHandler(displayHandler, jbCefBrowser.cefBrowser)
        awaitClose {
            if (jbCefBrowser.isDisposed) return@awaitClose
            jbCefBrowser.jbCefClient.removeLoadHandler(loadHandler, jbCefBrowser.cefBrowser)
            jbCefBrowser.jbCefClient.removeDisplayHandler(displayHandler, jbCefBrowser.cefBrowser)
        }
    }

    override fun dispose() = Disposer.dispose(jbCefBrowser)

    private companion object {
        const val BLANK_PAGE = "about:blank"
        const val HTTP_SCHEME = "http"
        const val NO_STATUS = 0
        const val FIRST_ERROR_STATUS = 400

        /** The page sets its title while it composes, which happens right after the load it belongs to. */
        val IDENTIFICATION_TIMEOUT: Duration = 5.seconds
    }
}
