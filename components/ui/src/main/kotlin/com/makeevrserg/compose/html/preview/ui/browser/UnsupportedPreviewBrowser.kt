package com.makeevrserg.compose.html.preview.ui.browser

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.swing.JComponent
import javax.swing.SwingConstants

/**
 * Shown when JCEF is unavailable, for example under Remote Development or a non-JetBrains runtime.
 * The preview can still be opened in an external browser through the tool window actions. Every
 * load is reported as finished at once, so the panel never waits for a page that cannot render.
 */
class UnsupportedPreviewBrowser : PreviewBrowser {
    private val loads = MutableSharedFlow<String>(extraBufferCapacity = LOAD_BUFFER)

    override val component: JComponent = JBLabel(UNSUPPORTED_MESSAGE, SwingConstants.CENTER).apply {
        border = JBUI.Borders.empty(MESSAGE_PADDING)
    }

    override val isDevToolsSupported: Boolean = false

    override fun load(url: String) {
        loads.tryEmit(url)
    }

    override fun reload() = Unit

    override fun openDevTools() = Unit

    override fun pageLoads(): Flow<String> = loads

    override fun dispose() = Unit

    private companion object {
        const val LOAD_BUFFER = 16
        const val MESSAGE_PADDING = 16
        const val UNSUPPORTED_MESSAGE =
            "<html><center>The embedded browser (JCEF) is not available in this IDE runtime.<br/>" +
                "Use <b>Open in Browser</b> to view the preview.</center></html>"
    }
}
