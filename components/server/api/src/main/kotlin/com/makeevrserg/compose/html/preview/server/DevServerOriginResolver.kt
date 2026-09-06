package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * Knows where the server of a module is expected to answer before its run says so: the port the plugin
 * chose for a webpack dev server, the one `.kobweb/conf.yaml` declares for a Kobweb server.
 */
class DevServerOriginResolver(
    private val kobwebConfReader: KobwebConfReader,
    private val healthCheck: DevServerHealthCheck
) {
    suspend fun expected(host: PreviewHost, options: DevServerLaunchOptions): String? {
        return when (host.kind) {
            DevServerKind.KOBWEB -> kobwebConfReader.readBaseUrl(host.directory)
            DevServerKind.WEBPACK -> options.devServerPort?.let { port -> "$LOCALHOST_ORIGIN$port" }
        }
    }

    /** The expected origin, but only when a server answers there right now. */
    suspend fun findAlive(host: PreviewHost, options: DevServerLaunchOptions): String? {
        val baseUrl = expected(host, options) ?: return null
        return baseUrl.takeIf { candidate -> healthCheck.isAlive(candidate) }
    }

    private companion object {
        const val LOCALHOST_ORIGIN = "http://localhost:"
    }
}
