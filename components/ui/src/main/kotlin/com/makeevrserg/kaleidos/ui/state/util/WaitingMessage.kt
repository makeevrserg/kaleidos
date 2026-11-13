package com.makeevrserg.kaleidos.ui.state.util

import com.makeevrserg.kaleidos.host.api.model.PreviewHost
import com.makeevrserg.kaleidos.preview.api.feature.model.PreviewState
import com.makeevrserg.kaleidos.server.api.model.DevServerState
import com.makeevrserg.kaleidos.ui.state.model.PreviewMessage
import com.makeevrserg.kaleidos.ui.theme.PreviewMessageTexts

internal fun waitingMessage(
    state: PreviewState,
    host: PreviewHost?,
    messageTexts: PreviewMessageTexts
): PreviewMessage {
    val starting = state.serverState as? DevServerState.Starting
    return when {
        host == null -> messageTexts.lookingForHost
        state.previewUrl != null -> messageTexts.pageLoading(host)
        starting?.host == host -> messageTexts.startingServer(host)
        else -> messageTexts.connecting(host)
    }
}
