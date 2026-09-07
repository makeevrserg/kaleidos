package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowser
import kotlinx.coroutines.flow.filter
import org.jetbrains.jewel.foundation.theme.JewelTheme

private const val HTTP_SCHEME = "http"
private const val BLANK_PAGE = "about:blank"

/**
 * Feeds the browser and reports its loads. Renders nothing: the page is painted by the browser
 * itself, this only drives it from the composition.
 */
@Composable
private fun PreviewPageLoads(
    browser: PreviewBrowser,
    url: String,
    onLoadStart: (url: String) -> Unit,
    onLoadEnd: (isReload: Boolean) -> Unit
) {
    val currentOnLoadStart by rememberUpdatedState(onLoadStart)
    val currentOnLoadEnd by rememberUpdatedState(onLoadEnd)
    // Same counter for every URL of one browser: the collector below outlives a URL change.
    val loads = remember(browser) { mutableIntStateOf(0) }
    // Subscribed before any load is requested, and kept subscribed: a handler added after the load
    // can miss its end and leave the footer waiting for a page that is already there.
    LaunchedEffect(browser) {
        browser.pageLoads()
            .filter { loadedUrl -> loadedUrl.startsWith(HTTP_SCHEME) }
            .collect { _ ->
                loads.intValue++
                currentOnLoadEnd(loads.intValue > 1)
            }
    }
    LaunchedEffect(browser, url) {
        loads.intValue = 0
        currentOnLoadStart(url)
        browser.load(url)
    }
    // A stale page must not survive a switch to a file that gets its own page later.
    DisposableEffect(browser) {
        onDispose { browser.load(BLANK_PAGE) }
    }
}

/**
 * Hosts the embedded browser. Swing components live in the interop layer above the Compose canvas,
 * so nothing can be drawn over the browser: the placeholders are drawn where it is not, and it is
 * kept mounted and only hidden while there is no page. Adding and removing it instead would tear the
 * browser down and build it again on every state change.
 *
 * @param url page to render; null while the tool window shows something else
 * @param unsupportedText shown in place of the page when the runtime has no embedded browser
 */
@Composable
internal fun PreviewPageView(
    browser: PreviewBrowser,
    url: String?,
    unsupportedText: String,
    onLoadStart: (url: String) -> Unit,
    onLoadEnd: (isReload: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val component = browser.component
    when {
        component != null -> SwingPanel(
            background = JewelTheme.globalColors.panelBackground,
            factory = { component },
            modifier = modifier,
            update = { hosted -> hosted.isVisible = url != null }
        )
        url != null -> PreviewPlaceholder(text = unsupportedText, isLoading = false, modifier = modifier)
    }
    if (url != null) {
        PreviewPageLoads(
            browser = browser,
            url = url,
            onLoadStart = onLoadStart,
            onLoadEnd = onLoadEnd
        )
    }
}
