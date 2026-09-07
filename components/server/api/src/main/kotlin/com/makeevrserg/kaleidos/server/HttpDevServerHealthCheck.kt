package com.makeevrserg.kaleidos.server

import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class HttpDevServerHealthCheck(
    private val ioContext: CoroutineContext,
    private val connectTimeout: Duration
) : DevServerHealthCheck {

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
     * error page still proves the process listens on the port. Only I/O failures mean "not alive";
     * cancellation and programming errors propagate.
     */
    override suspend fun isAlive(url: String): Boolean = withContext(ioContext) {
        try {
            val connection = openConnection(url)
            connection.responseCode
            connection.disconnect()
            true
        } catch (_: IOException) {
            false
        }
    }
}
