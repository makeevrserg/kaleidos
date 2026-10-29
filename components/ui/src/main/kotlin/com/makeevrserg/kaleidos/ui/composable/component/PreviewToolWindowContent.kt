package com.makeevrserg.kaleidos.ui.composable.component

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
import com.makeevrserg.kaleidos.feature.api.PreviewStore
import com.makeevrserg.kaleidos.feature.model.PreviewState
import com.makeevrserg.kaleidos.ui.browser.api.PreviewBrowser
import com.makeevrserg.kaleidos.ui.composable.component.PreviewPageView
import com.makeevrserg.kaleidos.ui.composable.component.PreviewPlaceholder
import com.makeevrserg.kaleidos.ui.composable.component.PreviewSourceBanner
import com.makeevrserg.kaleidos.ui.composable.component.PreviewStatusBar
import com.makeevrserg.kaleidos.ui.state.model.PreviewContent
import com.makeevrserg.kaleidos.ui.state.model.PreviewPlaceholderKind
import com.makeevrserg.kaleidos.ui.state.model.PreviewUiState
import com.makeevrserg.kaleidos.ui.theme.PreviewMessageTexts
import org.jetbrains.jewel.foundation.theme.JewelTheme

private const val CONTENT_TRANSITION = "preview-content"
private const val CONTENT_WEIGHT = 1f

@Composable
private fun LoadingContent(content: PreviewContent.Loading) {
    PreviewPlaceholder(
        message = content.message,
        kind = PreviewPlaceholderKind.LOADING,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun EmptyContent(content: PreviewContent.Empty) {
    PreviewPlaceholder(
        message = content.message,
        kind = PreviewPlaceholderKind.INFO,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun FailureContent(content: PreviewContent.Failure) {
    PreviewPlaceholder(
        message = content.message,
        kind = PreviewPlaceholderKind.FAILURE,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Content of the tool window: a spinner while the file is scanned, the preview module is looked up,
 * its dev server starts or the page loads, the page once the browser reports it, a banner while the
 * sources are newer than the page, and the state of the server in the footer.
 *
 * Rendered from the last state seen while the tool window was visible. The store keeps working while
 * nobody looks at the page, and dropping a loaded page for a state change no one can see would cost
 * a full dev server round trip to get it back.
 *
 * @param onDonateClick opens the ways of supporting the author, offered by the footer
 */
@Composable
internal fun PreviewToolWindowContent(
    store: PreviewStore,
    browser: PreviewBrowser,
    messageTexts: PreviewMessageTexts,
    buildUiState: (PreviewState) -> PreviewUiState,
    donationText: String,
    onDonateClick: () -> Unit
) {
    val storeState by store.state.collectAsState()
    var visibleState by remember { mutableStateOf(storeState) }
    LaunchedEffect(storeState) {
        if (!storeState.isToolWindowVisible) return@LaunchedEffect
        visibleState = storeState
    }

    val uiState = remember(visibleState) { buildUiState.invoke(visibleState) }

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
                unsupportedMessage = messageTexts.unsupportedBrowser,
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
                    is PreviewContent.Loading -> LoadingContent(content)
                    is PreviewContent.Empty -> EmptyContent(content)
                    is PreviewContent.Failure -> FailureContent(content)
                }
            }
        }
        PreviewStatusBar(
            text = uiState.statusText,
            donationText = donationText,
            onDonateClick = onDonateClick
        )
    }
}
