package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.host.PreviewHost

/**
 * Stops only servers that are up, exactly as the real stopper does with the record of a module, and a
 * server it stopped stops answering: the process is gone and its port is free.
 */
class FakeDetachedServerStopper(private val healthCheck: FakeDevServerHealthCheck) : DetachedServerStopper {
    val runningServers = mutableMapOf<PreviewHost, String>()

    val stoppedHosts = mutableListOf<PreviewHost>()

    override suspend fun stop(host: PreviewHost) {
        val baseUrl = runningServers.remove(host) ?: return
        healthCheck.aliveUrls -= baseUrl
        stoppedHosts += host
    }
}
