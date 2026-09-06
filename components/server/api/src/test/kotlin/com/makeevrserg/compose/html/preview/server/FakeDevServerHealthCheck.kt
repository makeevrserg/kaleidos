package com.makeevrserg.compose.html.preview.server

class FakeDevServerHealthCheck : DevServerHealthCheck {
    val aliveUrls = mutableSetOf<String>()

    override suspend fun isAlive(url: String): Boolean = url in aliveUrls
}
