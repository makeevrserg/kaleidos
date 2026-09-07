package com.makeevrserg.kaleidos.feature

import com.makeevrserg.kaleidos.host.PreviewHost
import com.makeevrserg.kaleidos.server.DevServerLaunchOptions

data class RunningRequest(
    val host: PreviewHost,
    val options: DevServerLaunchOptions,
    val retryAfterFailure: Boolean
)
