package com.makeevrserg.kaleidos.server.api

interface DevServerHealthCheck {
    /** True when something answers at [url] right now. */
    suspend fun isAlive(url: String): Boolean
}
