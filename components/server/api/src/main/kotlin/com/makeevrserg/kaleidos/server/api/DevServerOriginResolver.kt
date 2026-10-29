package com.makeevrserg.kaleidos.server.api

import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.server.model.DevServerLaunchOptions

interface DevServerOriginResolver {
    suspend fun expected(host: PreviewHost, options: DevServerLaunchOptions): String?

    suspend fun findAlive(host: PreviewHost, options: DevServerLaunchOptions): String?

    suspend fun findForeign(host: PreviewHost, options: DevServerLaunchOptions): String?
}
