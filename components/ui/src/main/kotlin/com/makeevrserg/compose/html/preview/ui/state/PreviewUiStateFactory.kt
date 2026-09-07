package com.makeevrserg.compose.html.preview.ui.state

import com.makeevrserg.compose.html.preview.feature.PageState
import com.makeevrserg.compose.html.preview.feature.PreviewScan
import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.ui.PreviewStateTexts

/** Turns the store state into the one state the tool window renders. */
class PreviewUiStateFactory(
    private val texts: PreviewStateTexts
) {

    private fun banner(sourceState: SourceState): PreviewBanner? {
        return when (sourceState) {
            SourceState.UpToDate -> null
            is SourceState.Changed -> PreviewBanner.Rebuilding(texts.changedBanner(sourceState))
            is SourceState.Reloaded -> PreviewBanner.Reloaded(texts.reloadedBanner(sourceState))
        }
    }

    /** What the selected file itself has to say; null once it has previews the page can render. */
    private fun scanContent(target: PreviewTarget): PreviewContent? {
        return when (val scan = target.scan) {
            PreviewScan.Pending -> PreviewContent.Loading(texts.scanning(target.fileName))
            PreviewScan.NoPreviews -> PreviewContent.Empty(texts.noPreviews(target.fileName))
            is PreviewScan.UnsupportedSourceSet ->
                PreviewContent.Empty(texts.unsupportedSourceSet(target.fileName, scan))
            is PreviewScan.Renderable -> null
        }
    }

    /**
     * Whatever keeps renderable previews from reaching the screen, nearest cause first; null while
     * nothing does.
     */
    private fun failureContent(state: PreviewState): PreviewContent? {
        val host = state.target?.host
        val serverState = state.serverState
        val pageState = state.pageState
        return when {
            host is PreviewHostResolution.NotFound -> PreviewContent.Failure(texts.hostNotFound(host))
            serverState is DevServerState.Failed -> PreviewContent.Failure(texts.serverFailed(serverState))
            pageState is PageState.Failed -> PreviewContent.Failure(texts.pageFailed(pageState))
            else -> null
        }
    }

    private fun content(state: PreviewState): PreviewContent {
        val target = state.target ?: return PreviewContent.Empty(texts.noSelectedFile)
        val scanContent = scanContent(target)
        if (scanContent != null) return scanContent
        val failureContent = failureContent(state)
        if (failureContent != null) return failureContent
        val previewUrl = state.previewUrl ?: return PreviewContent.Loading(texts.connecting(state))
        if (state.pageState !is PageState.Shown) return PreviewContent.Loading(texts.pageLoading)
        return PreviewContent.Page(previewUrl)
    }

    /** The footer stays silent while the content already explains everything. */
    private fun statusText(state: PreviewState, content: PreviewContent): String {
        return when (content) {
            is PreviewContent.Empty -> ""
            is PreviewContent.Failure -> ""
            is PreviewContent.Loading -> texts.status(state.serverState, previewUrl = null)
            is PreviewContent.Page -> texts.status(state.serverState, content.url)
        }
    }

    fun create(state: PreviewState): PreviewUiState {
        val content = content(state)
        return PreviewUiState(
            banner = banner(state.sourceState),
            content = content,
            pageUrl = state.previewUrl,
            statusText = statusText(state, content)
        )
    }
}
