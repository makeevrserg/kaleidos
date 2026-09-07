package com.makeevrserg.compose.html.preview.ui.state

import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.SourceState
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

    private fun content(state: PreviewState): PreviewContent {
        val message = texts.message(state)
        val previewUrl = state.previewUrl
        return when {
            message != null -> PreviewContent.Message(message)
            previewUrl == null -> PreviewContent.Loading(texts.loading(state))
            else -> PreviewContent.Page(previewUrl)
        }
    }

    /** The footer stays silent while the message already explains everything. */
    private fun statusText(state: PreviewState, content: PreviewContent): String {
        return when (content) {
            is PreviewContent.Message -> ""
            is PreviewContent.Loading -> texts.status(state.serverState, previewUrl = null)
            is PreviewContent.Page -> texts.status(state.serverState, content.url)
        }
    }

    fun create(state: PreviewState): PreviewUiState {
        val content = content(state)
        return PreviewUiState(
            banner = banner(state.sourceState),
            content = content,
            statusText = statusText(state, content)
        )
    }
}
