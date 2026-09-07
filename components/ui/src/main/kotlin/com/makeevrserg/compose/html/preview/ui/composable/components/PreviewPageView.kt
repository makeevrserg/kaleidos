package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.makeevrserg.compose.html.preview.feature.PageLoad
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowser
import com.makeevrserg.compose.html.preview.ui.state.PreviewMessage
import org.jetbrains.jewel.foundation.theme.JewelTheme

private const val NO_PAGE_SIZE = 0

/**
 * Feeds the browser and reports its loads. Renders nothing: the page is painted by the browser
 * itself, this only drives it from the composition.
 */
@Composable
private fun PreviewPageLoads(
    browser: PreviewBrowser,
    url: String?,
    onLoad: (PageLoad) -> Unit
) {
    val currentOnLoad by rememberUpdatedState(onLoad)
    // Subscribed before the first load and kept subscribed for the life of the browser: a handler
    // added after a load can miss its end and leave the tool window waiting for a page already there.
    LaunchedEffect(browser) {
        browser.loads().collect { load -> currentOnLoad(load) }
    }
    LaunchedEffect(browser, url) {
        if (url == null) browser.clear() else browser.load(url)
    }
}

/**
 * Hosts the embedded browser, which is a Swing component of a Compose panel and therefore a hole cut
 * into the canvas: whatever is drawn where it sits is replaced by it, and hiding the component does
 * not fill the hole back in, it only leaves the background of the window showing through. So while
 * there is no page the browser is given no room at all, which is what keeps a message from turning
 * into an empty rectangle. It stays in the hierarchy: an off-screen browser disposes its rendering
 * on `removeNotify`, and nothing has to be loaded while it has no room anyway.
 *
 * @param url page to load; null while the tool window shows a message instead
 * @param isPageVisible whether the page is on screen, or a message is shown in its place
 * @param unsupportedMessage shown in place of the page when the runtime has no embedded browser
 */
@Composable
internal fun PreviewPageView(
    browser: PreviewBrowser,
    url: String?,
    isPageVisible: Boolean,
    unsupportedMessage: PreviewMessage,
    onLoad: (PageLoad) -> Unit,
    modifier: Modifier = Modifier
) {
    val component = browser.component
    when {
        component != null -> SwingPanel(
            background = JewelTheme.globalColors.panelBackground,
            factory = { component },
            modifier = if (isPageVisible) modifier else Modifier.size(NO_PAGE_SIZE.dp)
        )
        isPageVisible -> PreviewPlaceholder(
            message = unsupportedMessage,
            kind = PreviewPlaceholderKind.INFO,
            modifier = modifier
        )
    }
    PreviewPageLoads(
        browser = browser,
        url = url,
        onLoad = onLoad
    )
}
