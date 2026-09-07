package com.makeevrserg.kaleidos.server

import java.net.URI

/** Origin of a dev server of this machine, without trailing slash: `http://localhost:8086`. */
internal object LocalhostOrigin {

    fun ofPort(port: Int): String = "http://localhost:$port"

    /** Null for an address that carries no port, which no origin built here does. */
    fun portOf(baseUrl: String): Int? {
        val port = runCatching { URI(baseUrl).port }.getOrNull() ?: return null
        return port.takeIf { value -> value > NO_PORT }
    }

    private const val NO_PORT = 0
}
