package com.makeevrserg.kaleidos.server.api.api

import com.makeevrserg.kaleidos.host.api.model.PreviewHost

/**
 * Stops a dev server that is a separate process outliving its Gradle run. Must not depend on the IDE
 * project: it is called while the project closes, when Gradle tasks can no longer be run in it.
 */
interface DetachedServerStopper {
    /** No-op when no server of [host] is running. */
    suspend fun stop(host: PreviewHost)
}
