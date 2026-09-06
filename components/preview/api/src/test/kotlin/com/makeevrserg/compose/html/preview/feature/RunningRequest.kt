package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.server.DevServerLaunchOptions

data class RunningRequest(
    val host: PreviewHost,
    val options: DevServerLaunchOptions,
    val retryAfterFailure: Boolean
)
