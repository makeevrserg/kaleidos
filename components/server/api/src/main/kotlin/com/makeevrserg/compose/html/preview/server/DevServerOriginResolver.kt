package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * Knows where the server of a module is expected to answer before its run says so. Only Kobweb can
 * tell, from its configuration file; webpack picks its port in the build script, so only the output of
 * the run reveals it.
 */
class DevServerOriginResolver(
    private val kobwebConfReader: KobwebConfReader,
    private val healthCheck: DevServerHealthCheck
) {
    suspend fun expected(host: PreviewHost): String? {
        return when (host.kind) {
            DevServerKind.KOBWEB -> kobwebConfReader.readBaseUrl(host.directory)
            DevServerKind.WEBPACK -> null
        }
    }

    /** The expected origin, but only when a server answers there right now. */
    suspend fun findAlive(host: PreviewHost): String? {
        val baseUrl = expected(host) ?: return null
        return baseUrl.takeIf { candidate -> healthCheck.isAlive(candidate) }
    }
}
