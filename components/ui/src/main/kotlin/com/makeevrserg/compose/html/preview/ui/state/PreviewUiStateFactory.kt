package com.makeevrserg.compose.html.preview.ui.state

import com.makeevrserg.compose.html.preview.feature.PageState
import com.makeevrserg.compose.html.preview.feature.PreviewScan
import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.ui.PreviewMessageTexts
import com.makeevrserg.compose.html.preview.ui.PreviewStatusTexts

/** Turns the store state into the one state the tool window renders. */
class PreviewUiStateFactory(
    private val statusTexts: PreviewStatusTexts,
    private val messageTexts: PreviewMessageTexts
) {

    private fun banner(sourceState: SourceState): PreviewBanner? {
        return when (sourceState) {
            SourceState.UpToDate -> null
            is SourceState.Changed -> PreviewBanner.Rebuilding(statusTexts.changedBanner(sourceState))
            is SourceState.Reloaded -> PreviewBanner.Reloaded(statusTexts.reloadedBanner(sourceState))
        }
    }

    /** What the selected file itself has to say; null once it has previews the page can render. */
    private fun scanContent(target: PreviewTarget): PreviewContent? {
        return when (val scan = target.scan) {
            PreviewScan.Pending -> PreviewContent.Loading(messageTexts.scanning(target.fileName))
            PreviewScan.NoPreviews -> PreviewContent.Empty(messageTexts.noPreviews(target.fileName))
            is PreviewScan.UnsupportedSourceSet ->
                PreviewContent.Empty(messageTexts.unsupportedSourceSet(target.fileName, scan))
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
            host is PreviewHostResolution.NotFound -> PreviewContent.Failure(messageTexts.hostNotFound(host))
            serverState is DevServerState.Failed -> PreviewContent.Failure(messageTexts.serverFailed(serverState))
            pageState is PageState.Failed -> PreviewContent.Failure(messageTexts.pageFailed(pageState))
            pageState is PageState.Foreign -> PreviewContent.Failure(messageTexts.foreignPage(serverState))
            else -> null
        }
    }

    /** What is being waited for while the previews of the file have no page yet. */
    private fun waitingMessage(state: PreviewState, host: PreviewHost?): PreviewMessage {
        val starting = state.serverState as? DevServerState.Starting
        return when {
            host == null -> messageTexts.lookingForHost
            state.previewUrl != null -> messageTexts.pageLoading(host)
            starting?.host == host -> messageTexts.startingServer(host)
            else -> messageTexts.connecting(host)
        }
    }

    /**
     * The address the browser should hold. A page that failed gives it up: the browser covers whatever
     * is drawn where it sits, and a message nobody can see is no message at all. Refresh Preview puts
     * it back.
     */
    private fun pageUrl(state: PreviewState): String? {
        return when (state.pageState) {
            PageState.Loading, PageState.Shown -> state.previewUrl
            PageState.Foreign, is PageState.Failed -> null
        }
    }

    private fun content(state: PreviewState, pageUrl: String?): PreviewContent {
        val target = state.target ?: return PreviewContent.Empty(messageTexts.noSelectedFile)
        val scanContent = scanContent(target)
        if (scanContent != null) return scanContent
        val failureContent = failureContent(state)
        if (failureContent != null) return failureContent
        if (pageUrl == null || state.pageState !is PageState.Shown) {
            return PreviewContent.Loading(waitingMessage(state, (target.host as? PreviewHostResolution.Found)?.host))
        }
        return PreviewContent.Page(pageUrl)
    }

    /** The footer stays silent while the content already explains everything. */
    private fun statusText(state: PreviewState, content: PreviewContent): String {
        return when (content) {
            is PreviewContent.Empty -> ""
            is PreviewContent.Failure -> ""
            is PreviewContent.Loading -> statusTexts.status(state.serverState, previewUrl = null)
            is PreviewContent.Page -> statusTexts.status(state.serverState, content.url)
        }
    }

    fun create(state: PreviewState): PreviewUiState {
        val pageUrl = pageUrl(state)
        val content = content(state, pageUrl)
        return PreviewUiState(
            banner = banner(state.sourceState),
            content = content,
            pageUrl = pageUrl,
            statusText = statusText(state, content)
        )
    }
}
