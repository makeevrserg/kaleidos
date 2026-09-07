package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * @param isRestart true when the request replaces the server of the module: the one that answers now
 * is what is being replaced, so it must not be adopted as the answer to the request
 * @param attempt distinguishes repeated requests for the same module, so each one is a new launch
 */
data class DevServerLaunchRequest(
    val host: PreviewHost,
    val options: DevServerLaunchOptions,
    val isRestart: Boolean,
    val attempt: Int
)
