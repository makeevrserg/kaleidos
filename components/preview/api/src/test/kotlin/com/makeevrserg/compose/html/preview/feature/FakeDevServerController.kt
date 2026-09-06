package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.server.DevServerController
import com.makeevrserg.compose.html.preview.server.DevServerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeDevServerController : DevServerController {
    val mutableState = MutableStateFlow<DevServerState>(DevServerState.Stopped)

    val ensureRunningRequests = mutableListOf<EnsureRunningRequest>()

    val restartedHosts = mutableListOf<PreviewHost>()

    var stopCount = 0

    override val state: StateFlow<DevServerState> = mutableState

    override suspend fun ensureRunning(host: PreviewHost, retryAfterFailure: Boolean) {
        ensureRunningRequests += EnsureRunningRequest(host, retryAfterFailure)
    }

    override suspend fun stop() {
        stopCount++
    }

    override suspend fun restart(host: PreviewHost) {
        restartedHosts += host
    }
}
