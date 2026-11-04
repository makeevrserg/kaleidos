package com.makeevrserg.kaleidos.server.api.api

import com.makeevrserg.kaleidos.host.api.model.PreviewHost
import com.makeevrserg.kaleidos.server.api.model.DevServerLaunchOptions

interface DevServerOriginResolver {
    suspend fun expected(host: PreviewHost, options: DevServerLaunchOptions): String?

    suspend fun findAlive(host: PreviewHost, options: DevServerLaunchOptions): String?

    suspend fun findForeign(host: PreviewHost, options: DevServerLaunchOptions): String?
}
