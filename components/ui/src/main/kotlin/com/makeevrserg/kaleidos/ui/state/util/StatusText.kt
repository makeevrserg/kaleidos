package com.makeevrserg.kaleidos.ui.state.util

import com.makeevrserg.kaleidos.preview.api.feature.model.PreviewState
import com.makeevrserg.kaleidos.ui.state.model.PreviewContent
import com.makeevrserg.kaleidos.ui.theme.PreviewStatusTexts

internal fun formatStatusText(
    state: PreviewState,
    content: PreviewContent,
    statusTexts: PreviewStatusTexts
): String {
    return when (content) {
        is PreviewContent.Empty -> ""
        is PreviewContent.Failure -> ""
        is PreviewContent.Loading -> statusTexts.status(state.serverState, previewUrl = null)
        is PreviewContent.Page -> statusTexts.status(state.serverState, content.url)
    }
}
