package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * Knows where the server of a module is expected to answer before its run says so: the port the plugin
 * chose for a webpack dev server, the one `.kobweb/conf.yaml` declares for a Kobweb server.
 *
 * An answer from that address is no proof that the server of the module gave it, which is what
 * [findAlive] and [findForeign] tell apart.
 */
class DevServerOriginResolver(
    private val kobwebConfReader: KobwebConfReader,
    private val kobwebServerStateReader: KobwebServerStateReader,
    private val healthCheck: DevServerHealthCheck
) {
    suspend fun expected(host: PreviewHost, options: DevServerLaunchOptions): String? {
        return when (host.kind) {
            DevServerKind.KOBWEB -> kobwebConfReader.readBaseUrl(host.directory)
            DevServerKind.WEBPACK -> options.devServerPort?.let(LocalhostOrigin::ofPort)
        }
    }

    /**
     * Whether the server of the module itself is what answers at [baseUrl].
     *
     * A Kobweb server outlives its run and shares the default port with every other Kobweb module of
     * the build, so only the process the module recorded counts: the server of another module, or of
     * another project, answers a health check just as well and serves a page that knows nothing of the
     * previews of this one. A webpack server dies with its run and records nothing, but the plugin
     * picks its port among the free ports of this machine, so that address stands for itself.
     */
    private suspend fun isOwnServer(host: PreviewHost, baseUrl: String): Boolean {
        return when (host.kind) {
            DevServerKind.KOBWEB -> {
                val running = kobwebServerStateReader.readRunning(host.directory)
                running != null && LocalhostOrigin.ofPort(running.port) == baseUrl
            }

            DevServerKind.WEBPACK -> true
        }
    }

    /** The expected origin, but only when the server of the module answers there right now. */
    suspend fun findAlive(host: PreviewHost, options: DevServerLaunchOptions): String? {
        val baseUrl = expected(host, options) ?: return null
        if (!isOwnServer(host, baseUrl)) return null
        return baseUrl.takeIf { candidate -> healthCheck.isAlive(candidate) }
    }

    /**
     * The expected origin when something that is not the server of the module already answers there:
     * its run could not bind that address, and would fail without saying why.
     */
    suspend fun findForeign(host: PreviewHost, options: DevServerLaunchOptions): String? {
        val baseUrl = expected(host, options) ?: return null
        if (isOwnServer(host, baseUrl)) return null
        return baseUrl.takeIf { candidate -> healthCheck.isAlive(candidate) }
    }
}
