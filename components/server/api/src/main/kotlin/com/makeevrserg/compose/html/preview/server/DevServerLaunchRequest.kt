package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * @param attempt distinguishes repeated requests for the same module, so each one is a new launch
 */
data class DevServerLaunchRequest(
    val host: PreviewHost,
    val options: DevServerLaunchOptions,
    val attempt: Int
)
