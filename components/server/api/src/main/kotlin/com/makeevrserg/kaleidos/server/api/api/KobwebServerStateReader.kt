package com.makeevrserg.kaleidos.server.api.api

import com.makeevrserg.kaleidos.server.api.model.RunningKobwebServer

interface KobwebServerStateReader {
    suspend fun readRunning(moduleDirectory: String): RunningKobwebServer?
}
