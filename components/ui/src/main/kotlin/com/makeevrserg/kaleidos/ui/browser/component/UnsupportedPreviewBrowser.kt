package com.makeevrserg.kaleidos.ui.browser.component

import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import com.makeevrserg.kaleidos.ui.browser.api.PreviewBrowser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.swing.JComponent

/**
 * Used when JCEF is unavailable, for example under Remote Development or a non-JetBrains runtime.
 * It has no surface of its own: the panel puts a message where the page would be, and the preview
 * can still be opened in an external browser through the tool window actions. Every load is
 * reported as finished at once, so the panel never waits for a page that cannot render.
 */
internal class UnsupportedPreviewBrowser : PreviewBrowser {
    override val component: JComponent? = null

    override val isDevToolsSupported: Boolean = false

    override fun reload() = Unit

    override fun openDevTools() = Unit

    override fun observePages(urls: Flow<String?>): Flow<PageLoad> {
        return urls.filterNotNull().map { PageLoad.Succeeded }
    }

    override fun dispose() = Unit
}
