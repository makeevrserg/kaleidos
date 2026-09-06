package com.makeevrserg.compose.html.preview.server

data class StartedRun(
    val config: DevServerLaunchConfig,
    val executionName: String,
    val listener: DevServerProcessListener
)
