package com.makeevrserg.kaleidos.feature

import com.makeevrserg.kaleidos.server.DevServerState

/**
 * @param previewUrl page for [target]; null until the dev server of the target's module is running
 * @param isToolWindowVisible rendering and dev server start happen only while true
 */
data class PreviewState(
    val target: PreviewTarget?,
    val previewUrl: String?,
    val isToolWindowVisible: Boolean,
    val sourceState: SourceState,
    val serverState: DevServerState,
    val pageState: PageState
)
