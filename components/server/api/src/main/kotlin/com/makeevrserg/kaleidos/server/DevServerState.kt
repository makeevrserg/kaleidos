package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.host.PreviewHost

sealed interface DevServerState {
    data object Stopped : DevServerState

    /** The Gradle task of [host] runs; its origin is learned from the output and polled until it answers. */
    data class Starting(val host: PreviewHost) : DevServerState

    /** @param baseUrl origin of the server without trailing slash, `http://localhost:8085` */
    data class Running(val host: PreviewHost, val baseUrl: String) : DevServerState

    data class Failed(val reason: String) : DevServerState
}
