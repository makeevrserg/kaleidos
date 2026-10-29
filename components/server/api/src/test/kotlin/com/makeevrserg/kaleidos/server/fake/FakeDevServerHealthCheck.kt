package com.makeevrserg.kaleidos.server.fake

import com.makeevrserg.kaleidos.server.api.DevServerHealthCheck

class FakeDevServerHealthCheck : DevServerHealthCheck {
    val aliveUrls = mutableSetOf<String>()

    override suspend fun isAlive(url: String): Boolean = url in aliveUrls
}
