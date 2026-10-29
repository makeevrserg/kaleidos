package com.makeevrserg.kaleidos.feature.model

import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.server.model.DevServerLaunchOptions

data class RunningRequest(
    val host: PreviewHost,
    val options: DevServerLaunchOptions,
    val retryAfterFailure: Boolean
)
