package com.makeevrserg.kaleidos.server

class FakeDevServerHealthCheck : DevServerHealthCheck {
    val aliveUrls = mutableSetOf<String>()

    override suspend fun isAlive(url: String): Boolean = url in aliveUrls
}
