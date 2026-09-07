package com.makeevrserg.compose.html.preview.ui.browser

import com.makeevrserg.compose.html.preview.feature.PageLoad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.swing.JComponent

/**
 * Used when JCEF is unavailable, for example under Remote Development or a non-JetBrains runtime.
 * It has no surface of its own: the panel puts a message where the page would be, and the preview
 * can still be opened in an external browser through the tool window actions. Every load is
 * reported as finished at once, so the panel never waits for a page that cannot render.
 */
class UnsupportedPreviewBrowser : PreviewBrowser {
    private val pageLoads = MutableSharedFlow<PageLoad>(extraBufferCapacity = LOAD_BUFFER)

    override val component: JComponent? = null

    override val isDevToolsSupported: Boolean = false

    override fun load(url: String) {
        pageLoads.tryEmit(PageLoad.Succeeded)
    }

    override fun reload() = Unit

    override fun openDevTools() = Unit

    override fun loads(): Flow<PageLoad> = pageLoads

    override fun dispose() = Unit

    private companion object {
        const val LOAD_BUFFER = 16
    }
}
