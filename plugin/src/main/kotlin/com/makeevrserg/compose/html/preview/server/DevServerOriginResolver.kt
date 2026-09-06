package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import java.util.concurrent.ConcurrentHashMap

/**
 * Knows where the server of a module answers. Origins announced by runs of this session are kept by
 * module directory: switching to a file of another module leaves the previous server running, and
 * switching back reuses it. Before the first run only Kobweb can tell, from its configuration file;
 * webpack picks its port in the build script, so only the output of the run reveals it.
 */
class DevServerOriginResolver(
    private val kobwebConfReader: KobwebConfReader,
    private val healthCheck: DevServerHealthCheck
) {
    private val knownBaseUrls = ConcurrentHashMap<String, String>()

    fun remember(host: PreviewHost, baseUrl: String) {
        knownBaseUrls[host.directory] = baseUrl
    }

    fun forget(host: PreviewHost) {
        knownBaseUrls.remove(host.directory)
    }

    suspend fun expected(host: PreviewHost): String? {
        return knownBaseUrls[host.directory] ?: when (host.kind) {
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
