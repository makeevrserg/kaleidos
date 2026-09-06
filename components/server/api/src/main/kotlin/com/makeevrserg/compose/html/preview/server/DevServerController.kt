package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the dev server of the current preview module. The server lives no longer than the scope of the
 * controller: closing the project stops every run the plugin started.
 */
interface DevServerController {
    val state: StateFlow<DevServerState>

    /**
     * Adopts a server that already answers or launches one. Returns as soon as the request is placed;
     * progress is reported through [state], so a caller that needs the origin awaits the first
     * [DevServerState.Running] there.
     *
     * @param retryAfterFailure true for explicit user requests such as Refresh, false for automatic
     * checks triggered by editor events; automatic checks never relaunch after a failed launch
     */
    suspend fun requestRunning(host: PreviewHost, retryAfterFailure: Boolean)

    /** Stops the server the plugin started for the current module; adopted servers keep running. */
    fun stop()

    fun restart(host: PreviewHost)
}
