package com.makeevrserg.compose.html.preview.harness

class FakeFreePortAllocator(private val port: Int) : FreePortAllocator {
    val keys = mutableListOf<String>()

    override fun allocate(key: String): Int {
        keys.add(key)
        return port
    }
}
