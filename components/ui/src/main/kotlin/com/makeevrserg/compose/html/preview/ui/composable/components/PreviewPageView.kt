package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.makeevrserg.compose.html.preview.feature.PageLoad
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowser
import org.jetbrains.jewel.foundation.theme.JewelTheme

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
        if (url != null) browser.load(url)
    }
}

/**
 * Hosts the embedded browser. Swing components live in the interop layer above the Compose canvas,
 * so nothing can be drawn over the browser: the placeholders are drawn where it is not, and it is
 * kept mounted and only hidden while it has nothing to show. Adding and removing it instead would
 * tear the browser down and build it again on every state change.
 *
 * A page is loaded while the browser is hidden and shown only once the store reports that load, which
 * is what keeps the page of the file left behind from ever being seen.
 *
 * @param url page to load; null while the tool window has no page for the selected file
 * @param isPageVisible whether the browser shows the page it was last asked to load
 * @param unsupportedText shown in place of the page when the runtime has no embedded browser
 */
@Composable
internal fun PreviewPageView(
    browser: PreviewBrowser,
    url: String?,
    isPageVisible: Boolean,
    unsupportedText: String,
    onLoad: (PageLoad) -> Unit,
    modifier: Modifier = Modifier
) {
    val component = browser.component
    when {
        component != null -> SwingPanel(
            background = JewelTheme.globalColors.panelBackground,
            factory = { component },
            modifier = modifier,
            update = { hosted -> hosted.isVisible = isPageVisible }
        )
        isPageVisible -> PreviewPlaceholder(
            text = unsupportedText,
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
