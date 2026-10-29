package com.makeevrserg.kaleidos.ui.state.util

import com.makeevrserg.kaleidos.feature.model.PageState
import com.makeevrserg.kaleidos.feature.model.PreviewScan
import com.makeevrserg.kaleidos.feature.model.PreviewState
import com.makeevrserg.kaleidos.feature.model.PreviewTarget
import com.makeevrserg.kaleidos.feature.model.SourceState
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.host.model.PreviewHostResolution
import com.makeevrserg.kaleidos.server.model.DevServerState
import com.makeevrserg.kaleidos.ui.state.model.PreviewBanner
import com.makeevrserg.kaleidos.ui.state.model.PreviewContent
import com.makeevrserg.kaleidos.ui.state.model.PreviewMessage
import com.makeevrserg.kaleidos.ui.state.model.PreviewUiState
import com.makeevrserg.kaleidos.ui.theme.PreviewMessageTexts
import com.makeevrserg.kaleidos.ui.theme.PreviewStatusTexts

private fun SourceState.toBanner(statusTexts: PreviewStatusTexts): PreviewBanner? {
    return when (this) {
        SourceState.UpToDate -> null
        is SourceState.Changed -> PreviewBanner.Rebuilding(statusTexts.changedBanner(this))
        is SourceState.Reloaded -> PreviewBanner.Reloaded(statusTexts.reloadedBanner(this))
    }
}

private fun PreviewTarget.scanContent(messageTexts: PreviewMessageTexts): PreviewContent? {
    return when (val scan = scan) {
        PreviewScan.Pending -> PreviewContent.Loading(messageTexts.scanning(fileName))
        PreviewScan.NoPreviews -> PreviewContent.Empty(messageTexts.noPreviews(fileName))
        is PreviewScan.UnsupportedSourceSet ->
            PreviewContent.Empty(messageTexts.unsupportedSourceSet(fileName, scan))
        is PreviewScan.Renderable -> null
    }
}

private fun PreviewState.failureContent(messageTexts: PreviewMessageTexts): PreviewContent? {
    val host = target?.host
    val serverState = serverState
    val pageState = pageState
    return when {
        host is PreviewHostResolution.NotFound -> PreviewContent.Failure(messageTexts.hostNotFound(host))
        serverState is DevServerState.Failed -> PreviewContent.Failure(messageTexts.serverFailed(serverState))
        pageState is PageState.Failed -> PreviewContent.Failure(messageTexts.pageFailed(pageState))
        pageState is PageState.Foreign -> PreviewContent.Failure(messageTexts.foreignPage(serverState))
        else -> null
    }
}

private fun PreviewState.waitingMessage(host: PreviewHost?, messageTexts: PreviewMessageTexts): PreviewMessage {
    val starting = serverState as? DevServerState.Starting
    return when {
        host == null -> messageTexts.lookingForHost
        previewUrl != null -> messageTexts.pageLoading(host)
        starting?.host == host -> messageTexts.startingServer(host)
        else -> messageTexts.connecting(host)
    }
}

private fun PreviewState.pageUrl(): String? {
    return when (pageState) {
        PageState.Loading, PageState.Shown -> previewUrl
        PageState.Foreign, is PageState.Failed -> null
    }
}

private fun PreviewState.content(
    pageUrl: String?,
    messageTexts: PreviewMessageTexts
): PreviewContent {
    val target = target ?: return PreviewContent.Empty(messageTexts.noSelectedFile)
    val scanContent = target.scanContent(messageTexts)
    if (scanContent != null) return scanContent
    val failureContent = failureContent(messageTexts)
    if (failureContent != null) return failureContent
    if (pageUrl == null || pageState !is PageState.Shown) {
        val host = (target.host as? PreviewHostResolution.Found)?.host
        return PreviewContent.Loading(waitingMessage(host, messageTexts))
    }
    return PreviewContent.Page(pageUrl)
}

private fun PreviewState.statusText(content: PreviewContent, statusTexts: PreviewStatusTexts): String {
    return when (content) {
        is PreviewContent.Empty -> ""
        is PreviewContent.Failure -> ""
        is PreviewContent.Loading -> statusTexts.status(serverState, previewUrl = null)
        is PreviewContent.Page -> statusTexts.status(serverState, content.url)
    }
}

internal fun PreviewState.buildUiState(
    statusTexts: PreviewStatusTexts,
    messageTexts: PreviewMessageTexts
): PreviewUiState {
    val pageUrl = pageUrl()
    val content = content(pageUrl, messageTexts)
    return PreviewUiState(
        banner = sourceState.toBanner(statusTexts),
        content = content,
        pageUrl = pageUrl,
        statusText = statusText(content, statusTexts)
    )
}
