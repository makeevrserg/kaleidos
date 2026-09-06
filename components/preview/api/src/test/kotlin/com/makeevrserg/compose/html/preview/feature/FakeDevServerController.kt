package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.server.DevServerController
import com.makeevrserg.compose.html.preview.server.DevServerLaunchOptions
import com.makeevrserg.compose.html.preview.server.DevServerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeDevServerController : DevServerController {
    val mutableState = MutableStateFlow<DevServerState>(DevServerState.Stopped)

    val runningRequests = mutableListOf<RunningRequest>()

    val restartedHosts = mutableListOf<PreviewHost>()

    var stopCount = 0

    override val state: StateFlow<DevServerState> = mutableState

    override suspend fun requestRunning(
        host: PreviewHost,
        options: DevServerLaunchOptions,
        retryAfterFailure: Boolean
    ) {
        runningRequests += RunningRequest(host, options, retryAfterFailure)
    }

    override fun stop() {
        stopCount++
    }

    override fun restart(host: PreviewHost, options: DevServerLaunchOptions) {
        restartedHosts += host
    }
}
