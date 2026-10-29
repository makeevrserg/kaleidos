package com.makeevrserg.kaleidos.server.api

interface KobwebConfReader {
    suspend fun readPort(moduleDirectory: String): Int
}
