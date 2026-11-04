package com.makeevrserg.kaleidos.ui.browser.component

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import com.makeevrserg.kaleidos.ui.browser.api.PreviewBrowser
import com.makeevrserg.kaleidos.ui.browser.model.BrowserPageEvent
import com.makeevrserg.kaleidos.ui.browser.util.observeBrowserPages
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import javax.swing.JComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class JcefPreviewBrowser(
    private val jbCefBrowser: JBCefBrowser
) : PreviewBrowser {
    override val component: JComponent = jbCefBrowser.component

    override val isDevToolsSupported: Boolean = true

    /** The panel may ask for a page while the tool window is closing the browser down. */
    private fun navigate(url: String?) {
        if (jbCefBrowser.isDisposed) return
        jbCefBrowser.loadURL(url ?: BLANK_PAGE)
    }

    private fun registerTitleHandler(events: SendChannel<BrowserPageEvent>): AutoCloseable {
        val handler = object : CefDisplayHandlerAdapter() {
            override fun onTitleChange(browser: CefBrowser?, title: String?) {
                events.trySend(BrowserPageEvent.TitleChanged(title))
            }
        }
        val client = jbCefBrowser.jbCefClient
        val browser = jbCefBrowser.cefBrowser
        client.addDisplayHandler(handler, browser)
        return AutoCloseable {
            if (!jbCefBrowser.isDisposed) client.removeDisplayHandler(handler, browser)
        }
    }

    private fun registerLoadHandler(events: SendChannel<BrowserPageEvent>): AutoCloseable {
        val loadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
                events.trySend(BrowserPageEvent.Started(frame.isMain))
            }

            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String?,
                failedUrl: String?
            ) {
                events.trySend(
                    BrowserPageEvent.Failed(
                        isMain = frame.isMain,
                        isAborted = errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED,
                        errorText = errorText,
                        errorCode = errorCode.name
                    )
                )
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                events.trySend(
                    BrowserPageEvent.Finished(
                        frame.isMain,
                        frame.url,
                        httpStatusCode
                    )
                )
            }
        }
        val client = jbCefBrowser.jbCefClient
        val browser = jbCefBrowser.cefBrowser
        client.addLoadHandler(loadHandler, browser)
        return AutoCloseable {
            if (!jbCefBrowser.isDisposed) client.removeLoadHandler(loadHandler, browser)
        }
    }

    override fun reload() = jbCefBrowser.cefBrowser.reload()

    override fun openDevTools() = jbCefBrowser.openDevtools()

    override fun observePages(urls: Flow<String?>): Flow<PageLoad> {
        return urls.observeBrowserPages(
            registerLoadHandler = ::registerLoadHandler,
            registerTitleHandler = ::registerTitleHandler,
            navigate = ::navigate,
            identificationTimeout = IDENTIFICATION_TIMEOUT
        )
    }

    override fun dispose() = Disposer.dispose(jbCefBrowser)

    private companion object {
        const val BLANK_PAGE = "about:blank"

        /** The page sets its title while it composes, right after the load it belongs to. */
        val IDENTIFICATION_TIMEOUT: Duration = 5.seconds
    }
}
