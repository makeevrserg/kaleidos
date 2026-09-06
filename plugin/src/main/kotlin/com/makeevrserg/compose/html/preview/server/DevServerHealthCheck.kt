package com.makeevrserg.compose.html.preview.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import kotlin.time.Duration

class DevServerHealthCheck(
    private val ioDispatcher: CoroutineDispatcher,
    private val connectTimeout: Duration
) {

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeout.inWholeMilliseconds.toInt()
        connection.readTimeout = connectTimeout.inWholeMilliseconds.toInt()
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = false
        return connection
    }

    /**
     * Any HTTP answer counts as alive: webpack-dev-server serves the page for every route, and an
     * error page still proves the process listens on the port.
     */
    suspend fun isAlive(url: String): Boolean = withContext(ioDispatcher) {
        runCatching {
            val connection = openConnection(url)
            connection.responseCode
            connection.disconnect()
        }.isSuccess
    }
}
