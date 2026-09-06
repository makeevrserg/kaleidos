package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost
import kotlinx.coroutines.flow.StateFlow

/** Owns the lifecycle of the dev server of the current preview module. */
interface DevServerController {
    val state: StateFlow<DevServerState>

    /**
     * Adopts a server that already answers or launches one, then waits until it answers or the
     * startup times out.
     *
     * @param retryAfterFailure true for explicit user requests such as Refresh, false for automatic
     * checks triggered by editor events; automatic checks never relaunch after a failed launch
     */
    suspend fun ensureRunning(host: PreviewHost, retryAfterFailure: Boolean)

    /** Stops the server of the current module only; servers of other modules keep running. */
    suspend fun stop()

    suspend fun restart(host: PreviewHost)
}
