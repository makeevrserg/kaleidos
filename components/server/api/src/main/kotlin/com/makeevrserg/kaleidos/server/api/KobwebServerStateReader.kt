package com.makeevrserg.kaleidos.server.api

import com.makeevrserg.kaleidos.server.model.RunningKobwebServer

interface KobwebServerStateReader {
    suspend fun readRunning(moduleDirectory: String): RunningKobwebServer?
}
