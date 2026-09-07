package com.makeevrserg.compose.html.preview.ui.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.ui.PreviewStateTexts
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowser
import com.makeevrserg.compose.html.preview.ui.composable.components.PreviewPageView
import com.makeevrserg.compose.html.preview.ui.composable.components.PreviewPlaceholder
import com.makeevrserg.compose.html.preview.ui.composable.components.PreviewPlaceholderKind
import com.makeevrserg.compose.html.preview.ui.composable.components.PreviewSourceBanner
import com.makeevrserg.compose.html.preview.ui.composable.components.PreviewStatusBar
import com.makeevrserg.compose.html.preview.ui.state.PreviewContent
import com.makeevrserg.compose.html.preview.ui.state.PreviewUiStateFactory
import org.jetbrains.jewel.foundation.theme.JewelTheme

private const val CONTENT_TRANSITION = "preview-content"
private const val CONTENT_WEIGHT = 1f

/**
 * Content of the tool window: a spinner while the file is scanned, the preview module is looked up,
 * its dev server starts or the page loads, the page once the browser reports it, a banner while the
 * sources are newer than the page, and the state of the server in the footer.
 *
 * Rendered from the last state seen while the tool window was visible. The store keeps working while
 * nobody looks at the page, and dropping a loaded page for a state change no one can see would cost
 * a full dev server round trip to get it back.
 */
@Composable
internal fun PreviewToolWindowContent(
    store: PreviewStore,
    browser: PreviewBrowser,
    texts: PreviewStateTexts,
    uiStateFactory: PreviewUiStateFactory
) {
    val storeState by store.state.collectAsState()
    var visibleState by remember { mutableStateOf(storeState) }
    LaunchedEffect(storeState) {
        if (storeState.isToolWindowVisible) visibleState = storeState
    }

    val uiState = remember(visibleState) { uiStateFactory.create(visibleState) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        PreviewSourceBanner(banner = uiState.banner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(CONTENT_WEIGHT)
        ) {
            PreviewPageView(
                browser = browser,
                url = uiState.pageUrl,
                isPageVisible = uiState.content is PreviewContent.Page,
                unsupportedText = texts.unsupportedBrowser,
                onLoad = store::onPageLoaded,
                modifier = Modifier.fillMaxSize()
            )
            AnimatedContent(
                targetState = uiState.content,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentAlignment = Alignment.Center,
                label = CONTENT_TRANSITION
            ) { content ->
                when (content) {
                    is PreviewContent.Page -> Box(modifier = Modifier.fillMaxSize())
                    is PreviewContent.Loading -> PreviewPlaceholder(
                        text = content.text,
                        kind = PreviewPlaceholderKind.LOADING,
                        modifier = Modifier.fillMaxSize()
                    )
                    is PreviewContent.Empty -> PreviewPlaceholder(
                        text = content.text,
                        kind = PreviewPlaceholderKind.INFO,
                        modifier = Modifier.fillMaxSize()
                    )
                    is PreviewContent.Failure -> PreviewPlaceholder(
                        text = content.text,
                        kind = PreviewPlaceholderKind.FAILURE,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        PreviewStatusBar(text = uiState.statusText)
    }
}
