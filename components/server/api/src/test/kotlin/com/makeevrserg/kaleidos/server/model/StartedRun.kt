package com.makeevrserg.kaleidos.server.model

import kotlinx.coroutines.channels.SendChannel

data class StartedRun(
    val config: DevServerLaunchConfig,
    val executionName: String,
    val events: SendChannel<GradleRunEvent>
)
