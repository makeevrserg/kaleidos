package com.makeevrserg.kaleidos.server.api.api

interface KobwebConfReader {
    suspend fun readPort(moduleDirectory: String): Int
}
