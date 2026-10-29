package com.makeevrserg.kaleidos.feature.fake

import com.makeevrserg.kaleidos.feature.model.RunningRequest
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.server.api.DevServerController
import com.makeevrserg.kaleidos.server.model.DevServerLaunchOptions
import com.makeevrserg.kaleidos.server.model.DevServerState
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
        runningRequests += RunningRequest(
            host,
            options,
            retryAfterFailure
        )
    }

    override fun stop() {
        stopCount++
    }

    override fun restart(host: PreviewHost, options: DevServerLaunchOptions) {
        restartedHosts += host
    }
}
