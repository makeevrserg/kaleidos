package com.makeevrserg.compose.html.preview.server

interface DevServerHealthCheck {
    /** True when something answers at [url] right now. */
    suspend fun isAlive(url: String): Boolean
}
