package com.makeevrserg.kaleidos.server.api.model

import com.makeevrserg.kaleidos.host.api.model.PreviewHost

data class RunningRequest(
    val host: PreviewHost,
    val options: DevServerLaunchOptions,
    val retryAfterFailure: Boolean
)
