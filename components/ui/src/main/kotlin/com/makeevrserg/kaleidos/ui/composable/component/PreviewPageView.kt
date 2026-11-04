package com.makeevrserg.kaleidos.ui.composable.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.makeevrserg.kaleidos.ui.browser.api.PreviewBrowser
import com.makeevrserg.kaleidos.ui.state.model.PreviewMessage
import com.makeevrserg.kaleidos.ui.state.model.PreviewPlaceholderKind
import org.jetbrains.jewel.foundation.theme.JewelTheme

private const val NO_PAGE_SIZE = 0

@Composable
private fun EmbeddedBrowserPanelComposable(
    component: javax.swing.JComponent,
    modifier: Modifier = Modifier
) {
    SwingPanel(
        background = JewelTheme.globalColors.panelBackground,
        factory = { component },
        modifier = modifier
    )
}

@Composable
private fun UnsupportedBrowserPlaceholderComposable(message: PreviewMessage, modifier: Modifier = Modifier) {
    PreviewPlaceholder(
        message = message,
        kind = PreviewPlaceholderKind.INFO,
        modifier = modifier
    )
}

/**
 * Hosts the embedded browser, which is a Swing component of a Compose panel and therefore a hole cut
 * into the canvas: whatever is drawn where it sits is replaced by it, and hiding the component does
 * not fill the hole back in, it only leaves the background of the window showing through. So while
 * there is no page the browser is given no room at all, which is what keeps a message from turning
 * into an empty rectangle. It stays in the hierarchy: an off-screen browser disposes its rendering
 * on `removeNotify`, and nothing has to be loaded while it has no room anyway.
 *
 * @param isPageVisible whether the page is on screen, or a message is shown in its place
 * @param unsupportedMessage shown in place of the page when the runtime has no embedded browser
 */
@Composable
internal fun PreviewPageView(
    browser: PreviewBrowser,
    isPageVisible: Boolean,
    unsupportedMessage: PreviewMessage,
    modifier: Modifier = Modifier
) {
    val component = browser.component
    val panelModifier = if (isPageVisible) modifier else Modifier.size(NO_PAGE_SIZE.dp)
    when {
        component != null -> EmbeddedBrowserPanelComposable(component, panelModifier)
        isPageVisible -> UnsupportedBrowserPlaceholderComposable(unsupportedMessage, modifier)
    }
}
