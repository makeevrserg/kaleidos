package com.makeevrserg.kaleidos.server.util

import java.net.URI

private const val NO_PORT = 0

internal fun localhostOrigin(port: Int): String = "http://localhost:$port"

internal fun localhostPort(baseUrl: String): Int? {
    val port = runCatching { URI(baseUrl).port }.getOrNull() ?: return null
    return port.takeIf { value -> value > NO_PORT }
}
